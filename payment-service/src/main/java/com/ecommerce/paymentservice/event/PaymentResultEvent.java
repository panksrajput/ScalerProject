package com.ecommerce.paymentservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultEvent {
    private Long orderId;
    private String transactionId;
    private Long paymentId;
    private boolean success;
}