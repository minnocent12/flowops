package com.flowops.order.service;

import com.flowops.order.dto.OrderRequest;
import com.flowops.order.dto.OrderResponse;
import com.flowops.order.messaging.OrderEventPublisher;
import com.flowops.order.model.Order;
import com.flowops.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final RestTemplate restTemplate;
    private final OrderEventPublisher publisher;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public OrderService(OrderRepository repo,
                        RestTemplate restTemplate,
                        OrderEventPublisher publisher) {
        this.repo = repo;
        this.restTemplate = restTemplate;
        this.publisher = publisher;
    }

    public OrderResponse createOrder(OrderRequest req) {
        String url = inventoryServiceUrl + "/api/inventory/check?sku="
                     + req.getSku() + "&quantity=" + req.getQuantity();
        Boolean inStock = restTemplate.getForObject(url, Boolean.class);

        Order order = new Order();
        order.setCustomerId(req.getCustomerId());
        order.setSku(req.getSku());
        order.setQuantity(req.getQuantity());

        boolean accepted = Boolean.TRUE.equals(inStock);
        order.setStatus(accepted ? "ACCEPTED" : "REJECTED_OUT_OF_STOCK");

        Order saved = repo.save(order);

        if (accepted) {
            publisher.publishOrderAccepted(
                saved.getId().toString(), saved.getSku(), saved.getQuantity()
            );
        }
        return toResponse(saved);
    }

    public OrderResponse getOrder(UUID id) {
        return toResponse(repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found: " + id)));
    }

    public List<OrderResponse> getAllOrders() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getSku(),
                                 o.getQuantity(), o.getStatus(), o.getCreatedAt());
    }
}
