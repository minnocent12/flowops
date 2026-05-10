package com.flowops.shipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private String carrier;

    @Column(name = "tracking_id", nullable = false, unique = true)
    private String trackingId;

    @Column(nullable = false)
    private String status = "CREATED";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & setters
    public UUID getId() { return id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String o) { this.orderId = o; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String w) { this.warehouseId = w; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String c) { this.carrier = c; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String t) { this.trackingId = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
