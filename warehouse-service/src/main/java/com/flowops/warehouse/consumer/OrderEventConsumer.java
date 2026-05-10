package com.flowops.warehouse.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.warehouse.model.OrderEvent;
import com.flowops.warehouse.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String OUT_TOPIC = "route.assigned";

    private final RoutingService routingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderEventConsumer(RoutingService routingService,
                              KafkaTemplate<String, String> kafkaTemplate) {
        this.routingService = routingService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order.accepted", groupId = "warehouse-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderAccepted(String message,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            OrderEvent event = mapper.readValue(message, OrderEvent.class);
            log.info("Received order.accepted for orderId={}", event.getOrderId());

            Optional<String> warehouse = routingService.findBestWarehouse(
                event.getSku(), event.getQuantity()
            );

            if (warehouse.isPresent()) {
                String payload = String.format(
                    "{\"orderId\":\"%s\",\"sku\":\"%s\",\"quantity\":%d,\"warehouseId\":\"%s\"}",
                    event.getOrderId(), event.getSku(),
                    event.getQuantity(), warehouse.get()
                );
                kafkaTemplate.send(OUT_TOPIC, event.getOrderId(), payload);
                log.info("Published route.assigned for orderId={} → warehouse={}",
                         event.getOrderId(), warehouse.get());
            } else {
                log.warn("No warehouse found for orderId={} sku={}",
                         event.getOrderId(), event.getSku());
            }

        } catch (Exception e) {
            log.error("Failed to process order.accepted: {}", e.getMessage());
        }
    }
}
