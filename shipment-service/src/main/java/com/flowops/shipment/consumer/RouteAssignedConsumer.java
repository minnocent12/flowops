package com.flowops.shipment.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RouteAssignedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RouteAssignedConsumer.class);

    private final ShipmentService shipmentService;
    private final RestTemplate    restTemplate;
    private final ObjectMapper    mapper = new ObjectMapper();

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public RouteAssignedConsumer(ShipmentService shipmentService, RestTemplate restTemplate) {
        this.shipmentService = shipmentService;
        this.restTemplate    = restTemplate;
    }

    @KafkaListener(topics = "route.assigned", groupId = "shipment-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleRouteAssigned(String message) {
        try {
            JsonNode node      = mapper.readTree(message);
            String orderId     = node.get("orderId").asText();
            String warehouseId = node.get("warehouseId").asText();
            String sku         = node.get("sku").asText();

            Shipment shipment = shipmentService.createShipment(orderId, warehouseId, sku);
            log.info("Shipment created: orderId={} trackingId={} carrier={}",
                     orderId, shipment.getTrackingId(), shipment.getCarrier());

            restTemplate.patchForObject(
                orderServiceUrl + "/api/orders/" + orderId + "/status?status=SHIPPED",
                null, Void.class);
            log.info("Order {} status → SHIPPED", orderId);

        } catch (Exception e) {
            log.error("Failed to process route.assigned: {}", e.getMessage());
        }
    }
}
