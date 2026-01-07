package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.client.*;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.dto.event.NotificationEvent;
import com.ecommerce.orderservice.dto.request.OrderPaymentInitRequest;
import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.PaymentStatusUpdateRequest;
import com.ecommerce.orderservice.dto.request.StockReduceRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.repository.OrderPaymentRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.EventPublisherService;
import com.ecommerce.orderservice.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ModelMapper modelMapper;
    private final ProductClient productClient;
    private final CartClient cartClient;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, Long userId) {
        logger.info("Creating order for user ID: {}", userId);
        try {
            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setUserId(userId);
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            processOrderItems(order, orderRequest.getItems());

            if (orderRequest.getBillingAddress() == null) {
                orderRequest.setBillingAddress(orderRequest.getShippingAddress());
            }

            order.setShippingAddress(mapToShippingAddress(orderRequest.getShippingAddress()));
            order.setBillingAddress(mapToBillingAddress(orderRequest.getBillingAddress()));

            order.calculateTotal();

            Order savedOrder = orderRepository.save(order);
            log.info("Created order {} for user {}", savedOrder.getOrderNumber(), userId);

            lockOrder(savedOrder.getId());

            // Publish notification event for order creation
            try {
                // Publish password reset email event
                NotificationEvent event = NotificationEvent.builder()
                        .type("ORDER_CREATED")
                        .recipient(order.getShippingAddress().getEmail())
                        .data(Map.of(
                                "orderNumber", order.getOrderNumber(),
                                "orderStatus", order.getStatus().toString()
                        ))
                        .build();

                eventPublisherService.publishNotification(event);
                logger.debug("Order created for user: {}", userId);
            } catch (Exception e) {
                logger.error("Failed to email for order creation: {}", e.getMessage(), e);
                // Don't fail registration if notification fails
            }

            return convertToDto(savedOrder);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Error creating order: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to create order: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to access this order");
        }
        
        return convertToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to access this order");
        }
        
        return convertToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::convertToDto);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, String status, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this order");
        }
        
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
            Order updatedOrder = orderRepository.save(order);

            // Publish notification event for email verification
            try {
                NotificationEvent event = NotificationEvent.builder()
                        .type("ORDER_STATUS_CHANGED")
                        .recipient(order.getShippingAddress().getEmail())
                        .data(Map.of(
                                "orderNumber", order.getOrderNumber(),
                                "orderStatus", order.getStatus().toString()
                        ))
                        .build();
                eventPublisherService.publishNotification(event);
                logger.debug("Order status changed for order: {}", orderNumber);
            } catch (Exception e) {
                logger.error("Failed to send email for order status update: {}", e.getMessage(), e);
            }
            
            return convertToDto(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid order status: " + status);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this order");
        }
        
        if (!order.getStatus().equals(Order.OrderStatus.PENDING)) {
            throw new OrderProcessingException("Only pending orders can be cancelled");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Publish notification event for email verification
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .type("ORDER_CANCELLED")
                    .recipient(order.getShippingAddress().getEmail())
                    .data(Map.of(
                            "orderNumber", order.getOrderNumber(),
                            "orderStatus", order.getStatus().toString()
                    ))
                    .build();
            eventPublisherService.publishNotification(event);
            logger.debug("Order cancelled for order: {}", orderNumber);
        } catch (Exception e) {
            logger.error("Failed to send order cancellation email: {}", e.getMessage(), e);
        }
    }

    @Override
    @Scheduled(fixedDelay = 60_000) // Run every 1 min
    @Transactional
    public void processExpiredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Order> expiredOrders = orderRepository.findOldOrdersByStatus(
            Order.OrderStatus.PENDING, 
            threshold
        );
        
        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired orders to cancel", expiredOrders.size());
            expiredOrders.forEach(order -> {
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                log.info("Cancelled expired order: {}", order.getOrderNumber());
            });
            orderRepository.saveAll(expiredOrders);
        }
    }

    // Helper methods
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void processOrderItems(Order order, List<OrderRequest.OrderItemRequest> items) {
        items.forEach(item -> {
            try {
                ProductDto product = productClient.getProduct(item.getProductId());

                // Create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductSku(product.getSku());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice());
                orderItem.setDiscountAmount(item.getDiscountAmount());
                orderItem.setTaxAmount(item.getTaxAmount());
                orderItem.calculateTotal();
                
                order.addItem(orderItem);
                
            } catch (FeignException.NotFound e) {
                throw new ResourceNotFoundException("Product not found with id: " + item.getProductId());
            }
        });
    }

    private com.ecommerce.orderservice.entity.ShippingAddress mapToShippingAddress(AddressDto dto) {
        if (dto == null) return null;
        
        com.ecommerce.orderservice.entity.ShippingAddress address = new com.ecommerce.orderservice.entity.ShippingAddress();
        address.setFirstName(dto.getFirstName());
        address.setLastName(dto.getLastName());
        address.setEmail(dto.getEmail());
        address.setPhone(dto.getPhone());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        address.setCompany(dto.getCompany());
        return address;
    }
    
    private com.ecommerce.orderservice.entity.BillingAddress mapToBillingAddress(AddressDto dto) {
        if (dto == null) return null;
        
        com.ecommerce.orderservice.entity.BillingAddress address = new com.ecommerce.orderservice.entity.BillingAddress();
        address.setFirstName(dto.getFirstName());
        address.setLastName(dto.getLastName());
        address.setEmail(dto.getEmail());
        address.setPhone(dto.getPhone());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        address.setCompany(dto.getCompany());
        return address;
    }

    private OrderResponse convertToDto(Order order) {
        OrderResponse dto = modelMapper.map(order, OrderResponse.class);
        dto.setItemCount(order.getItems().size());
        dto.setCanBeCancelled(order.getStatus() == Order.OrderStatus.PENDING);
        dto.setCanBeReturned(order.getStatus() == Order.OrderStatus.DELIVERED && 
                           order.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(30)));
        return dto;
    }

    @Transactional
    @Override
    public OrderResponse processOrderFromCart(OrderRequest orderRequest, Long userId) {
        log.info("Processing order from cart for user: {}", userId);

        // Get the cart
        CartDto cart;
        try {
            log.debug("Fetching cart for user id: {}", userId);
            cart = cartClient.getCart();
            
            log.debug("Retrieved cart with {} items", cart != null && cart.getItems() != null ? cart.getItems().size() : 0);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch cart: " + e.getMessage();
            log.error(errorMsg, e);
            throw new OrderProcessingException(errorMsg);
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.error("Cart is empty or invalid");
            throw new OrderProcessingException("Cart is empty or invalid");
        }

        // Create order request from cart
        OrderRequest newOrderRequest = new OrderRequest();

        // Map cart items to order items
        List<OrderRequest.OrderItemRequest> orderItems = cart.getItems().stream()
                .map(item -> {
                    OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest();
                    orderItem.setProductId(item.getProductId());
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setUnitPrice(item.getProductPrice());
                    // Calculate subtotal if not provided
                    BigDecimal subtotal = item.getTotalPrice() != null ?
                            item.getTotalPrice() :
                            item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    orderItem.setSubtotal(subtotal);
                    return orderItem;
                })
                .collect(Collectors.toList());

        newOrderRequest.setItems(orderItems);

        log.info("Creating order for user ID 4: {}", userId);
        newOrderRequest.setShippingAddress(orderRequest.getShippingAddress());
        log.info("Creating order for user ID 5: {}", userId);
        newOrderRequest.setBillingAddress(orderRequest.getBillingAddress());


        if (orderRequest.getPaymentMethod() == null || orderRequest.getPaymentMethod().trim().isEmpty()) {
            log.error("No payment method provided and no default payment method found");
            throw new OrderProcessingException("Payment method is required");
        }
        newOrderRequest.setPaymentMethod(orderRequest.getPaymentMethod());

        // Create the order
        OrderResponse order;
        try {
            log.info("Creating order from cart items");
            order = createOrder(newOrderRequest, userId);
            log.info("Successfully created order with ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to create order: " + e.getMessage());
        }

        // Clear the cart after successful order creation
        try {
            log.debug("Clearing cart after successful order creation");

            cartClient.clearCart();
            log.info("Successfully cleared cart for user id: {}", userId);

        } catch (Exception e) {
            log.error("Failed to clear cart after order creation: {}", e.getMessage(), e);
            // We can still proceed even if clearing the cart fails
        }

        // Publish notification event for order creation
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .type("ORDER_CREATED")
                    .recipient(order.getShippingAddress().getEmail())
                    .data(Map.of(
                            "orderNumber", order.getOrderNumber(),
                            "orderStatus", order.getStatus().toString()
                    ))
                    .build();
            eventPublisherService.publishNotification(event);
            logger.debug("Order created for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to email for order creation: {}", e.getMessage(), e);
            // Don't fail registration if notification fails
        }

        return order;
    }

    @Override
    @Transactional
    public OrderResponse updateShippingInfo(String orderNumber, String trackingNumber, String shippingMethod, Long userId) {
        logger.info("Updating shipping info for order: {}", orderNumber);
        
        // Find the order
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        
        // Check if the user is authorized to update this order
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this order");
        }
        
        // Update shipping information
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber.trim());
        }
        
        if (shippingMethod != null && !shippingMethod.trim().isEmpty()) {
            order.setShippingMethod(shippingMethod.trim());
        }
        
        // Update the order status to SHIPPED if it's not already
        if (order.getStatus() != Order.OrderStatus.SHIPPED) {
            order.setStatus(Order.OrderStatus.SHIPPED);
        }
        
        // Save the updated order
        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully updated shipping info for order: {}", orderNumber);
        
        return convertToDto(updatedOrder);
    }

    @Transactional
    public void updatePaymentStatus(PaymentStatusUpdateRequest req) {
        OrderPayment payment = orderPaymentRepository.findByPaymentId(req.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(req.getStatus());
        payment.setPaymentTxnId(req.getPaymentTxnId());
        orderPaymentRepository.save(payment);
    }

    @Transactional
    public void initPayment(OrderPaymentInitRequest req) {
        orderPaymentRepository.save(OrderPayment.builder()
                .orderId(req.getOrderId())
                .paymentId(req.getPaymentId())
                .status("INITIATED")
                .build());
    }

    public Order lockOrder(Long orderId) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setLocked(true);
        order.setLockedAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING);

        return orderRepository.save(order);
    }

    @Transactional
    public void unlockOrder(Long orderId, Long paymentId, String transactionId, boolean paymentSuccess) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (paymentSuccess) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.PROCESSING);
            order.setLocked(false);
            order.setLockedAt(null);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            Set<StockReduceRequest> stockReduceRequests = order.getItems()
                    .stream()
                    .map(item -> new StockReduceRequest(
                            item.getProductId(),
                            item.getQuantity()
                    )).collect(Collectors.toSet());

            productClient.reduceStockBatch(stockReduceRequests);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setStatus(Order.OrderStatus.PENDING);
            order.setLocked(false);
            order.setLockedAt(null);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        orderRepository.save(order);

        OrderPayment payment = orderPaymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(paymentSuccess?Order.PaymentStatus.PAID.toString():Order.PaymentStatus.FAILED.toString());
        payment.setPaymentTxnId(transactionId);
        orderPaymentRepository.save(payment);


    }

}
