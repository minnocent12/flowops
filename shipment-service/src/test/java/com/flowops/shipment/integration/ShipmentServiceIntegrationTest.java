package com.flowops.shipment.integration;

import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"route.assigned"},
    bootstrapServersProperty = "kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:shipmenttest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DirtiesContext
class ShipmentServiceIntegrationTest {

    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @AfterEach
    void cleanup() {
        shipmentRepository.deleteAll();
    }

    @Test
    @DisplayName("Shipment is created when route.assigned event is received")
    void shipmentCreated_whenRouteAssignedReceived() {
        String orderId = UUID.randomUUID().toString();

        kafkaTemplate.send("route.assigned", orderId, routeEvent(orderId, "WH-ATLANTA"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Shipment> shipments = shipmentRepository.findAll();
            assertThat(shipments).hasSize(1);
            assertThat(shipments.get(0).getOrderId()).isEqualTo(orderId);
            assertThat(shipments.get(0).getWarehouseId()).isEqualTo("WH-ATLANTA");
            assertThat(shipments.get(0).getTrackingId()).startsWith("TRK-");
            assertThat(shipments.get(0).getCarrier()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Duplicate route.assigned events create only one shipment (idempotency)")
    void idempotency_duplicateEventsProduceOneShipment() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String payload = routeEvent(orderId, "WH-DALLAS");

        kafkaTemplate.send("route.assigned", orderId, payload);
        kafkaTemplate.send("route.assigned", orderId, payload);

        await().atMost(10, SECONDS)
               .untilAsserted(() -> assertThat(shipmentRepository.findAll()).isNotEmpty());

        Thread.sleep(2_000);
        assertThat(shipmentRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Carrier is consistently assigned for the same warehouse")
    void carrierAssignment_isConsistentForSameWarehouse() {
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();

        kafkaTemplate.send("route.assigned", orderId1, routeEvent(orderId1, "WH-CHICAGO"));
        kafkaTemplate.send("route.assigned", orderId2, routeEvent(orderId2, "WH-CHICAGO"));

        await().atMost(10, SECONDS)
               .untilAsserted(() -> assertThat(shipmentRepository.findAll()).hasSize(2));

        List<Shipment> shipments = shipmentRepository.findAll();
        assertThat(shipments.get(0).getCarrier()).isEqualTo(shipments.get(1).getCarrier());
    }

    private String routeEvent(String orderId, String warehouseId) {
        return String.format(
            "{\"orderId\":\"%s\",\"sku\":\"SKU-001\",\"quantity\":10,\"warehouseId\":\"%s\"}",
            orderId, warehouseId);
    }
}
