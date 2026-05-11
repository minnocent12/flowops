package com.flowops.inventory.service;

import com.flowops.inventory.model.Inventory;
import com.flowops.inventory.repository.InventoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository repo;

    public InventoryService(InventoryRepository repo) {
        this.repo = repo;
    }

    @Cacheable(value = "inventory-check", key = "#sku + '-' + #requestedQty")
    public boolean isInStock(String sku, int requestedQty) {
        int total = repo.totalStockBySku(sku);
        return total >= requestedQty;
    }

    @Cacheable(value = "inventory-by-sku", key = "#sku")
    public List<Inventory> getStockBySku(String sku) {
        return repo.findBySku(sku);
    }

    public List<Inventory> getAllInventory() {
        return repo.findAll();
    }

    @Transactional
    @CacheEvict(value = {"inventory-check", "inventory-by-sku"}, allEntries = true)
    public boolean deductStock(String warehouseId, String sku, int qty) {
        int rows = repo.deductStock(warehouseId, sku, qty);
        return rows > 0;
    }
}
