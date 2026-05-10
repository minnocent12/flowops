package com.flowops.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String TOPIC = "order.accepted";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderAccepted(String orderId, String sku, int quantity) {
        String payload = String.format(
            "{\"orderId\":\"%s\",\"sku\":\"%s\",\"quantity\":%d}",
            orderId, sku, quantity
        );
        kafkaTemplate.send(TOPIC, orderId, payload);
        log.info("Published order.accepted event for orderId={}", orderId);
    }
}
