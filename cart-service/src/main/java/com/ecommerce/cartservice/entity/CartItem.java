package com.ecommerce.cartservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private Long productId;
    private String productName;
    private BigDecimal productPrice = BigDecimal.ZERO;
    private Integer quantity = 1;
    private String imageUrl;
    private String sku;
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
        calculateTotalPrice();
    }

    public void incrementQuantity(int quantity) {
        this.quantity += quantity;
        calculateTotalPrice();
    }

    private void calculateTotalPrice() {
        if (productPrice == null) {
            this.totalPrice = BigDecimal.ZERO;
            return;
        }
        this.totalPrice = productPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
