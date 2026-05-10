package com.flowops.warehouse.service;

import com.flowops.warehouse.client.InventoryClient;
import com.flowops.warehouse.model.WarehouseStock;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoutingService {

    private final InventoryClient inventoryClient;

    public RoutingService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    public Optional<String> findBestWarehouse(String sku, int requiredQty) {
        List<WarehouseStock> stocks = inventoryClient.getStockBySku(sku);
        return stocks.stream()
            .filter(s -> s.getQuantity() >= requiredQty)
            .max(Comparator.comparingInt(WarehouseStock::getQuantity)
                           .thenComparing(Comparator.comparing(WarehouseStock::getWarehouseId).reversed()))
            .map(WarehouseStock::getWarehouseId);
    }
}
