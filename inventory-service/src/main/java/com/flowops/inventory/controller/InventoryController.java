package com.flowops.inventory.controller;

import com.flowops.inventory.model.Inventory;
import com.flowops.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory check and lookup endpoints")
public class InventoryController {

    private final InventoryService svc;

    public InventoryController(InventoryService svc) {
        this.svc = svc;
    }

    @GetMapping("/check")
    @Operation(
        summary = "Check stock availability",
        description = "Returns true if enough stock exists across all warehouses for the given SKU and quantity"
    )
    public boolean checkStock(@RequestParam String sku,
                              @RequestParam int quantity) {
        return svc.isInStock(sku, quantity);
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get stock levels by SKU", description = "Returns stock per warehouse for a given SKU")
    public List<Inventory> getStockBySku(@PathVariable String sku) {
        return svc.getStockBySku(sku);
    }

    @GetMapping
    @Operation(summary = "List all inventory", description = "Returns all inventory records across all warehouses")
    public List<Inventory> getAllInventory() {
        return svc.getAllInventory();
    }
}
