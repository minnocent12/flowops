package com.flowops.order.controller;

import com.flowops.order.dto.OrderRequest;
import com.flowops.order.dto.OrderResponse;
import com.flowops.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService svc;

    public OrderController(OrderService svc) {
        this.svc = svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order", description = "Validates inventory and creates a new order")
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest req) {
        return svc.createOrder(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return svc.getOrder(id);
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public List<OrderResponse> getAllOrders() {
        return svc.getAllOrders();
    }
}
