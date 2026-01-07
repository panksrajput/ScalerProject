package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.event.NotificationEvent;

public interface EventPublisherService {
    void publishNotification(NotificationEvent event);
}
