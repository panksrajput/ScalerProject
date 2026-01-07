package com.ecommerce.paymentservice.entity;

import javax.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String txnId;
    private Double amount;
    private String productInfo;
    private String firstname;
    private String email;

    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payuResponse;
}

