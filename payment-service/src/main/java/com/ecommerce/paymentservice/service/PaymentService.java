package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;

public interface PaymentService {

    public Payment createTransaction(Double amount, String email, String firstname);
    public String generateHash(Payment tx, String orderId, String paymentId);
    public void updateStatus(String txnId, String status, String response);

}