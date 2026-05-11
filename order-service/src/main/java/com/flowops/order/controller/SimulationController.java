package com.flowops.order.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);
    private static volatile boolean inventoryDown = false;

    @PostMapping("/inventory-down")
    public Map<String, String> simulateInventoryDown() {
        inventoryDown = true;
        log.warn("SIMULATION: Inventory service marked as DOWN");
        return Map.of("status", "Inventory service simulated as DOWN");
    }

    @PostMapping("/inventory-up")
    public Map<String, String> simulateInventoryUp() {
        inventoryDown = false;
        log.info("SIMULATION: Inventory service marked as UP");
        return Map.of("status", "Inventory service restored");
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of("inventoryServiceDown", inventoryDown);
    }

    public static boolean isInventoryDown() { return inventoryDown; }
}
