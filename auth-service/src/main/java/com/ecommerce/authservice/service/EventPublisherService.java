package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.event.NotificationEvent;

public interface EventPublisherService {
    void publishNotification(NotificationEvent event);
}
