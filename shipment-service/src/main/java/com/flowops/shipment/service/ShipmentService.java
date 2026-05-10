package com.flowops.shipment.service;

import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ShipmentService {

    private static final String[] CARRIERS = {"UPS", "FEDEX", "USPS", "DHL"};
    private final ShipmentRepository repo;

    public ShipmentService(ShipmentRepository repo) {
        this.repo = repo;
    }

    public Shipment createShipment(String orderId, String warehouseId, String sku) {
        return repo.findByOrderId(orderId).orElseGet(() -> {
            Shipment s = new Shipment();
            s.setOrderId(orderId);
            s.setWarehouseId(warehouseId);
            s.setCarrier(assignCarrier(warehouseId));
            s.setTrackingId("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return repo.save(s);
        });
    }

    public List<Shipment> getAllShipments() {
        return repo.findAll();
    }

    private String assignCarrier(String warehouseId) {
        return CARRIERS[Math.abs(warehouseId.hashCode()) % CARRIERS.length];
    }
}
