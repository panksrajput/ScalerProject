package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dto.event.NotificationEvent;
import com.ecommerce.authservice.service.EventPublisherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherServiceImpl implements EventPublisherService {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String notificationTopic;

    public EventPublisherServiceImpl(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            @Value("${kafka.topics.notification:notification-topic}") String notificationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationTopic = notificationTopic;
    }

    public void publishNotification(NotificationEvent event) {
        kafkaTemplate.send(notificationTopic, event);
    }
}
