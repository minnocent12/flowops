package com.flowops.order.controller;

import com.flowops.order.metrics.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Order service metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    @Operation(summary = "Get order metrics")
    public Map<String, Object> getMetrics() {
        return metricsService.getMetrics();
    }
}
