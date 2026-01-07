package com.ecommerce.cartservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartDto {

    private Long userId;

    private List<CartItemDto> items = new ArrayList<>();

    private BigDecimal totalPrice;

    private Integer totalItems;

    private boolean empty;
}
