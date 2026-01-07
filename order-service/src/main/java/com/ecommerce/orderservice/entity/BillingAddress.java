package com.ecommerce.orderservice.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@Embeddable
public class BillingAddress {
    
    @Column(name = "billing_first_name")
    private String firstName;
    
    @Column(name = "billing_last_name")
    private String lastName;
    
    @Column(name = "billing_email")
    private String email;
    
    @Column(name = "billing_phone")
    private String phone;
    
    @Column(name = "billing_address_line1")
    private String addressLine1;
    
    @Column(name = "billing_address_line2")
    private String addressLine2;
    
    @Column(name = "billing_city")
    private String city;
    
    @Column(name = "billing_state")
    private String state;
    
    @Column(name = "billing_postal_code")
    private String postalCode;
    
    @Column(name = "billing_country")
    private String country;
    
    @Column(name = "billing_company")
    private String company;
    
    @Column(name = "tax_id")
    private String taxId;
}
