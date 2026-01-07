package com.ecommerce.orderservice.dto.request;

import com.ecommerce.orderservice.dto.AddressDto;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;
    
    @NotNull(message = "Shipping address is required")
    private AddressDto shippingAddress;
    
    private AddressDto billingAddress;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    
    private String notes;
    
    @Data
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private BigDecimal unitPrice;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal subtotal;
    }
}
