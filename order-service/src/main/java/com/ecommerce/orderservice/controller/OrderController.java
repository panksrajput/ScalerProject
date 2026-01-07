package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.OrderPaymentInitRequest;
import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.PaymentStatusUpdateRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderProcessingException;
import com.ecommerce.orderservice.exception.UnauthorizedAccessException;
import com.ecommerce.orderservice.security.CurrentUserResolver;
import com.ecommerce.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final CurrentUserResolver currentUserResolver;

    public OrderController(OrderService orderService,
                          CurrentUserResolver currentUserResolver) {
        this.orderService = orderService;
        this.currentUserResolver = currentUserResolver;
    }

    private Long getCurrentUserId() {
        return currentUserResolver.resolve().getUserId();
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        Long userId = getCurrentUserId();
        OrderResponse createdOrder = orderService.createOrder(orderRequest, userId);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdOrder.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(createdOrder);
    }

    @PostMapping("/from-cart")
    public ResponseEntity<?> createOrderFromCart(
            @RequestBody OrderRequest orderRequest) {
        logger.info("Creating order from cart");
        try {

            Long userId = getCurrentUserId();
            logger.debug("Processing order for user: {}", userId);

            OrderResponse order = orderService.processOrderFromCart(orderRequest, userId);
            logger.info("Successfully created order from cart for user id: {}", userId);
            return ResponseEntity.ok(order);

        } catch (OrderProcessingException e) {
            logger.error("Order processing failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing order from cart: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error processing order: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        OrderResponse order = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @PathVariable String orderNumber) {
        Long userId = getCurrentUserId();
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderNumber}/status/{status}")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderNumber,
            @PathVariable String status) {
        Long userId = getCurrentUserId();
        OrderResponse updatedOrder = orderService.updateOrderStatus(orderNumber, status, userId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderNumber) {
        try {
            Long userId = getCurrentUserId();
            // Update the order status to CANCELLED
            OrderResponse cancelledOrder = orderService.updateOrderStatus(orderNumber, "CANCELLED", userId);
            return ResponseEntity.ok(cancelledOrder);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (OrderProcessingException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{orderNumber}/shipping")
    public ResponseEntity<OrderResponse> updateShippingInfo(
            @PathVariable String orderNumber,
            @RequestParam String trackingNumber,
            @RequestParam String shippingMethod) {
        Long userId = getCurrentUserId();
        OrderResponse updatedOrder = orderService.updateShippingInfo(orderNumber, trackingNumber, shippingMethod, userId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/payment/status")
    public ResponseEntity<Void> updatePaymentStatus(
            @RequestBody PaymentStatusUpdateRequest request) {
        orderService.updatePaymentStatus(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payment/init")
    public ResponseEntity<Void> initPayment(
            @RequestBody OrderPaymentInitRequest req) {
        orderService.initPayment(req);
        return ResponseEntity.ok().build();
    }
}
