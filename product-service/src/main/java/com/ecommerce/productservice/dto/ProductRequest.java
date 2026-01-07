package com.ecommerce.productservice.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must be less than 100 characters")
    private String sku;

    private Set<String> imageUrls;

    @Size(max = 100, message = "Category name must be less than 100 characters")
    private String categoryName;

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
