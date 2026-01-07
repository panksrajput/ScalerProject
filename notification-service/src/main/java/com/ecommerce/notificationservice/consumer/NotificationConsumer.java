package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.dto.NotificationEvent;
import com.ecommerce.notificationservice.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @Autowired
    private EmailService emailService;

    @KafkaListener(
            topics = "${kafka.topics.notification}",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotification(@Payload NotificationEvent event) {
        log.info("Received notification event: {}", event);

        try {
            switch (event.getType()) {
                case "EMAIL_VERIFICATION":
                    emailService.sendVerificationEmail(
                            event.getRecipient(),
                            (String) event.getData().get("verificationToken"),
                            (String) event.getData().get("appUrl")
                    );
                    break;

                case "PASSWORD_RESET":
                    emailService.sendPasswordResetEmail(
                            event.getRecipient(),
                            (String) event.getData().get("verificationToken"),
                            (String) event.getData().get("appUrl")
                    );
                    break;

                case "ORDER_CREATED":
                    emailService.sendOrderCreatedEmail(
                            event.getRecipient(),
                            (String) event.getData().get("orderNumber"),
                            (String) event.getData().get("orderStatus")
                    );
                    break;

                case "ORDER_STATUS_CHANGED":
                    emailService.sendOrderStatusChangedEmail(
                            event.getRecipient(),
                            (String) event.getData().get("orderNumber"),
                            (String) event.getData().get("orderStatus")
                    );
                    break;

                case "ORDER_CANCELLED":
                    emailService.sendOrderCancelledEmail(
                            event.getRecipient(),
                            (String) event.getData().get("orderNumber"),
                            (String) event.getData().get("orderStatus")
                    );
                    break;

                default:
                    log.warn("Unknown notification type: {}", event.getType());
            }

            log.info("Successfully processed notification for: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
            // In a production environment, you might want to implement a dead-letter queue here
        }
    }
}