package com.ecommerce.notificationservice.service;

public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken, String appUrl);
    void sendPasswordResetEmail(String to, String resetToken, String appUrl);
    void sendOrderCreatedEmail(String to, String orderNumber, String orderStatus);
    void sendOrderStatusChangedEmail(String to, String orderNumber, String orderStatus);
    void sendOrderCancelledEmail(String to, String orderNumber, String orderStatus);
}