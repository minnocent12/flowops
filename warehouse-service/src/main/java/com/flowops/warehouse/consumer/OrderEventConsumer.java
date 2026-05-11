package com.flowops.warehouse.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.warehouse.client.InventoryClient;
import com.flowops.warehouse.model.OrderEvent;
import com.flowops.warehouse.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String OUT_TOPIC = "route.assigned";

    private final RoutingService     routingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final InventoryClient    inventoryClient;
    private final RestTemplate       restTemplate;
    private final ObjectMapper       mapper = new ObjectMapper();

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public OrderEventConsumer(RoutingService routingService,
                              KafkaTemplate<String, String> kafkaTemplate,
                              InventoryClient inventoryClient,
                              RestTemplate restTemplate) {
        this.routingService  = routingService;
        this.kafkaTemplate   = kafkaTemplate;
        this.inventoryClient = inventoryClient;
        this.restTemplate    = restTemplate;
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
                String warehouseId = warehouse.get();

                inventoryClient.deductStock(warehouseId, event.getSku(), event.getQuantity());
                log.info("Deducted {} x {} from {}", event.getQuantity(), event.getSku(), warehouseId);

                String payload = String.format(
                    "{\"orderId\":\"%s\",\"sku\":\"%s\",\"quantity\":%d,\"warehouseId\":\"%s\"}",
                    event.getOrderId(), event.getSku(), event.getQuantity(), warehouseId
                );
                kafkaTemplate.send(OUT_TOPIC, event.getOrderId(), payload);
                log.info("Published route.assigned for orderId={} → warehouse={}", event.getOrderId(), warehouseId);

                restTemplate.patchForObject(
                    orderServiceUrl + "/api/orders/" + event.getOrderId() + "/status?status=ROUTED",
                    null, Void.class);
                log.info("Order {} status → ROUTED", event.getOrderId());

            } else {
                log.warn("No warehouse found for orderId={} sku={}", event.getOrderId(), event.getSku());
            }

        } catch (Exception e) {
            log.error("Failed to process order.accepted: {}", e.getMessage());
        }
    }
}
