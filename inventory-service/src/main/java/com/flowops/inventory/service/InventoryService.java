package com.flowops.inventory.service;

import com.flowops.inventory.model.Inventory;
import com.flowops.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository repo;

    public InventoryService(InventoryRepository repo) {
        this.repo = repo;
    }

    public boolean isInStock(String sku, int requestedQty) {
        int total = repo.totalStockBySku(sku);
        return total >= requestedQty;
    }

    public List<Inventory> getStockBySku(String sku) {
        return repo.findBySku(sku);
    }

    public List<Inventory> getAllInventory() {
        return repo.findAll();
    }
}
