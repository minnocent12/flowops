package com.flowops.shipment.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RouteAssignedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RouteAssignedConsumer.class);
    private final ShipmentService shipmentService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RouteAssignedConsumer(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @KafkaListener(topics = "route.assigned", groupId = "shipment-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleRouteAssigned(String message) {
        try {
            JsonNode node = mapper.readTree(message);
            String orderId     = node.get("orderId").asText();
            String warehouseId = node.get("warehouseId").asText();
            String sku         = node.get("sku").asText();

            Shipment shipment = shipmentService.createShipment(orderId, warehouseId, sku);
            log.info("Shipment created: orderId={} trackingId={} carrier={}",
                     orderId, shipment.getTrackingId(), shipment.getCarrier());

        } catch (Exception e) {
            log.error("Failed to process route.assigned: {}", e.getMessage());
        }
    }
}
