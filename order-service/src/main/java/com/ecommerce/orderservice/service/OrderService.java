package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.request.OrderPaymentInitRequest;
import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.PaymentStatusUpdateRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public interface OrderService {
    
    OrderResponse createOrder(OrderRequest orderRequest, Long userId);
    
    OrderResponse getOrderById(Long orderId, Long userId);
    
    OrderResponse getOrderByOrderNumber(String orderNumber, Long userId);
    
    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    OrderResponse updateOrderStatus(String orderNumber, String status, Long userId);

    void cancelOrder(String orderNumber, Long userId);

    OrderResponse processOrderFromCart(OrderRequest orderRequest, Long userId);

    void processExpiredOrders();
    
    OrderResponse updateShippingInfo(String orderNumber, String trackingNumber, String shippingMethod, Long userId);

    public void updatePaymentStatus(PaymentStatusUpdateRequest req);

    public void initPayment(OrderPaymentInitRequest req);

    void unlockOrder(Long orderId, Long paymentId, String transactionId, boolean success);
}
