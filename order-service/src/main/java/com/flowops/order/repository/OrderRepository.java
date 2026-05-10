package com.flowops.order.repository;

import com.flowops.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(String status);
}
