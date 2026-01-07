package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.entity.Category;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String sku;
    private Set<String> imageUrls;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Float score; // For search relevance score

    // Specification fields
    private String brand;
    private String model;
    private String color;
    private String size;
    private String weight;
    private String dimensions;
    private String material;
    private String warranty;
    private String countryOfOrigin;
    private BigDecimal shippingWeight;
    private String manufacturer;
    private String careInstructions;
    private String includedComponents;
    private String recommendedAgeRange;
}
