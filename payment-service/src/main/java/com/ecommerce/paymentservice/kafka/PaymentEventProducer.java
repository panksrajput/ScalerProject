package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, PaymentResultEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-result}")
    private String topic;

    public void publish(PaymentResultEvent event) {
        logger.info("Publishing PaymentResultEvent: {}", event);

        kafkaTemplate.send(
                topic,
                event.getOrderId().toString(), // key (important)
                event
        );
    }
}
