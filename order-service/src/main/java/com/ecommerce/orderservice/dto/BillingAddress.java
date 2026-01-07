package com.ecommerce.orderservice.dto;

import lombok.Data;

@Data
public class BillingAddress {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
