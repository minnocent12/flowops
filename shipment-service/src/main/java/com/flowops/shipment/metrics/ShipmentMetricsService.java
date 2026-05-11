package com.flowops.shipment.metrics;

import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShipmentMetricsService {

    private final ShipmentRepository repo;

    public ShipmentMetricsService(ShipmentRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> getMetrics() {
        List<Shipment> all = repo.findAll();

        Map<String, Long> byWarehouse = all.stream()
            .collect(Collectors.groupingBy(Shipment::getWarehouseId, Collectors.counting()));

        Map<String, Long> byCarrier = all.stream()
            .collect(Collectors.groupingBy(Shipment::getCarrier, Collectors.counting()));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalShipments",       all.size());
        m.put("shipmentsByWarehouse", byWarehouse);
        m.put("shipmentsByCarrier",   byCarrier);
        m.put("generatedAt",          Instant.now().toString());
        return m;
    }
}
