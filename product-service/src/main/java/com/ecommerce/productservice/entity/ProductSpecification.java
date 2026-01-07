package com.ecommerce.productservice.entity;

import lombok.Data;

import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
@Data
public class ProductSpecification {
    private String brand;
    private String model;
    private String color;
    private String size;
    private String weight;
    private String dimensions; // e.g., "10x5x2 inches"
    private String material;
    private String warranty;
    private String countryOfOrigin;
    private BigDecimal shippingWeight;
    private String manufacturer;
    private String careInstructions;
    private String includedComponents;
    private String recommendedAgeRange;
    
    // Additional specifications can be added based on product category
}
