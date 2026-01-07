package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.OrderItemDto;
import com.ecommerce.orderservice.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private Order.PaymentStatus paymentStatus;
    private List<OrderItemDto> items;
    private AddressDto shippingAddress;
    private AddressDto billingAddress;
    private String paymentMethod;
    private String paymentId;
    private String shippingMethod;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for display
    private String formattedTotal;
    private String formattedDate;
    private int itemCount;
    private boolean canBeCancelled;
    private boolean canBeReturned;
}
