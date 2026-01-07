package com.ecommerce.cartservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDto {

    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private String imageUrl;
    private String sku;
    private BigDecimal totalPrice;
}
