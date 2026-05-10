package com.flowops.shipment.controller;

import com.flowops.shipment.model.Shipment;
import com.flowops.shipment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipments", description = "Shipment tracking endpoints")
public class ShipmentController {

    private final ShipmentService svc;

    public ShipmentController(ShipmentService svc) {
        this.svc = svc;
    }

    @GetMapping
    @Operation(summary = "List all shipments")
    public List<Shipment> getAllShipments() {
        return svc.getAllShipments();
    }
}
