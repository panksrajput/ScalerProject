package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private static final String VERIFICATION_TEMPLATE = "emails/verification-email";
    private static final String PASSWORD_RESET_TEMPLATE = "emails/password-reset-email";
    private static final String ORDER_CREATED_TEMPLATE = "emails/order-created-email";
    private static final String ORDER_STATUS_CHANGED_TEMPLATE = "emails/order-status-changed-email";
    private static final String ORDER_CANCELLED_TEMPLATE = "emails/order-cancelled-email";

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    @Async
    public void sendVerificationEmail(String to, String verificationToken, String appUrl) {
        try {
            String verificationUrl = appUrl + "/api/auth/verify-email?token=" + verificationToken;
            Context context = new Context();
            context.setVariable("verificationUrl", verificationUrl);

            String content = templateEngine.process(VERIFICATION_TEMPLATE, context);
            sendEmail(to, "Verify your email address", content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetToken, String appUrl) {
        try {
            String resetUrl = appUrl + "/reset-password?token=" + resetToken;
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);

            String content = templateEngine.process(PASSWORD_RESET_TEMPLATE, context);
            sendEmail(to, "Password Reset Request", content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    @Async
    public void sendOrderCreatedEmail(String to, String orderNumber, String orderStatus) {
        try {
            Context context = new Context();
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("orderStatus", orderStatus);

            String content = templateEngine.process(ORDER_CREATED_TEMPLATE, context);
            sendEmail(to, "Order has been placed", content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send order creation email", e);
        }
    }

    @Override
    @Async
    public void sendOrderStatusChangedEmail(String to, String orderNumber, String orderStatus) {
        try {
            Context context = new Context();
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("orderStatus", orderStatus);

            String content = templateEngine.process(ORDER_STATUS_CHANGED_TEMPLATE, context);
            sendEmail(to, "Order status has been changed", content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send order status change email", e);
        }
    }

    @Override
    @Async
    public void sendOrderCancelledEmail(String to, String orderNumber, String orderStatus) {
        try {
            Context context = new Context();
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("orderStatus", orderStatus);

            String content = templateEngine.process(ORDER_CANCELLED_TEMPLATE, context);
            sendEmail(to, "Order has been cancelled", content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send order creation email", e);
        }
    }

    private void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }
}