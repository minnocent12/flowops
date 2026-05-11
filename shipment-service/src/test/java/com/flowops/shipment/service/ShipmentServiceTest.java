package com.flowops.shipment.service;

import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock ShipmentRepository repo;
    @InjectMocks ShipmentService shipmentService;

    @Test
    @DisplayName("Creates new shipment when none exists for order")
    void createsShipmentForNewOrder() {
        when(repo.findByOrderId("order-123")).thenReturn(Optional.empty());
        Shipment saved = new Shipment();
        saved.setOrderId("order-123");
        saved.setWarehouseId("WH-DALLAS");
        saved.setCarrier("DHL");
        saved.setTrackingId("TRK-ABCD1234");
        when(repo.save(any())).thenReturn(saved);

        Shipment result = shipmentService.createShipment("order-123", "WH-DALLAS", "SKU-001");

        assertThat(result.getOrderId()).isEqualTo("order-123");
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("Returns existing shipment without saving again (idempotency)")
    void idempotentOnDuplicateEvent() {
        Shipment existing = new Shipment();
        existing.setOrderId("order-123");
        existing.setTrackingId("TRK-EXISTING");
        when(repo.findByOrderId("order-123")).thenReturn(Optional.of(existing));

        Shipment result = shipmentService.createShipment("order-123", "WH-DALLAS", "SKU-001");

        assertThat(result.getTrackingId()).isEqualTo("TRK-EXISTING");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Assigns consistent carrier based on warehouse ID")
    void assignsConsistentCarrier() {
        when(repo.findByOrderId(any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shipment r1 = shipmentService.createShipment("order-A", "WH-DALLAS", "SKU-001");
        Shipment r2 = shipmentService.createShipment("order-B", "WH-DALLAS", "SKU-001");

        assertThat(r1.getCarrier()).isEqualTo(r2.getCarrier());
    }
}
