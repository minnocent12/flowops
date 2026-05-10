package com.flowops.inventory.repository;

import com.flowops.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findBySku(String sku);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.sku = :sku")
    int totalStockBySku(String sku);
}
