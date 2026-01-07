package com.ecommerce.orderservice.dto.request;

import lombok.Data;

@Data
public class PaymentStatusUpdateRequest {
    private Long paymentId;
    private String paymentTxnId;
    private String status;     // SUCCESS / FAILED
}
