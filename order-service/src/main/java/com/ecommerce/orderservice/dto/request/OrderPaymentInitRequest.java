package com.ecommerce.orderservice.dto.request;

import lombok.Data;

@Data
public class OrderPaymentInitRequest {

    private Long orderId;
    private Long paymentId;
    private String txnId;
    private String paymentStatus; // INITIATED, SUCCESS, FAILED
}
