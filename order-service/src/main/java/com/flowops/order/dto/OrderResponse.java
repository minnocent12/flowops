package com.flowops.order.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderResponse {
    private UUID id;
    private String customerId;
    private String sku;
    private int quantity;
    private String status;
    private LocalDateTime createdAt;

    public OrderResponse(UUID id, String customerId, String sku, int quantity, String status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
