package com.ecommerce.orderservice.consumer;

import com.ecommerce.orderservice.dto.event.PaymentResultEvent;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentResultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PaymentResultConsumer.class);

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.payment-result:payment-result-topic}", groupId = "order-service")
    public void handle(PaymentResultEvent event) {
        logger.info("Received PaymentResultEvent: {}", event);

        orderService.unlockOrder(event.getOrderId(), event.getPaymentId(), event.getTransactionId(), event.isSuccess());
    }
}
