package com.ecommerce.orderservice.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@Embeddable
public class ShippingAddress {
    
    @Column(name = "shipping_first_name", nullable = false)
    private String firstName;
    
    @Column(name = "shipping_last_name", nullable = false)
    private String lastName;
    
    @Column(name = "shipping_email", nullable = false)
    private String email;
    
    @Column(name = "shipping_phone")
    private String phone;
    
    @Column(name = "shipping_address_line1", nullable = false)
    private String addressLine1;
    
    @Column(name = "shipping_address_line2")
    private String addressLine2;
    
    @Column(name = "shipping_city", nullable = false)
    private String city;
    
    @Column(name = "shipping_state")
    private String state;
    
    @Column(name = "shipping_postal_code", nullable = false)
    private String postalCode;
    
    @Column(name = "shipping_country", nullable = false)
    private String country;
    
    @Column(name = "shipping_company")
    private String company;
}
