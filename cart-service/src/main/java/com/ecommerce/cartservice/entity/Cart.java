package com.ecommerce.cartservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "carts")
public class Cart implements Serializable {

    @Id
    private String id;

    private Long userId;

    private List<CartItem> items = new ArrayList<>();

    private BigDecimal totalPrice = BigDecimal.ZERO;
    private int totalItems;
    private Boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void updateTotals() {
        totalPrice = BigDecimal.ZERO;
        totalItems = 0;

        for (CartItem item : items) {
            totalPrice = totalPrice.add(item.getTotalPrice());
            totalItems += item.getQuantity();
        }
        updatedAt = LocalDateTime.now();
    }
}
