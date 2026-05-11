package com.flowops.order.metrics;

import com.flowops.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MetricsService {

    private final OrderRepository repo;

    public MetricsService(OrderRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> getMetrics() {
        long total    = repo.count();
        long accepted = repo.findByStatus("ACCEPTED").size();
        long rejected = repo.findByStatus("REJECTED_OUT_OF_STOCK").size();
        double acceptRate = total > 0 ? Math.round(accepted * 1000.0 / total) / 10.0 : 0.0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalOrders",    total);
        m.put("acceptedOrders", accepted);
        m.put("rejectedOrders", rejected);
        m.put("acceptanceRate", acceptRate);
        m.put("generatedAt",    Instant.now().toString());
        return m;
    }
}
