package com.ecommerce.cartservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price = BigDecimal.ZERO;
    private String sku;
    private String imageUrl;
    private boolean inStock;
}
