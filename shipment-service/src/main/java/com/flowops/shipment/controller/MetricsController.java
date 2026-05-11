package com.flowops.shipment.controller;

import com.flowops.shipment.metrics.ShipmentMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Shipment service metrics")
public class MetricsController {

    private final ShipmentMetricsService metricsService;

    public MetricsController(ShipmentMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    @Operation(summary = "Get shipment metrics")
    public Map<String, Object> getMetrics() {
        return metricsService.getMetrics();
    }
}
