package com.flowops.order.service;

import com.flowops.order.dto.OrderRequest;
import com.flowops.order.dto.OrderResponse;
import com.flowops.order.messaging.OrderEventPublisher;
import com.flowops.order.model.Order;
import com.flowops.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository      repo;
    @Mock RestTemplate         restTemplate;
    @Mock OrderEventPublisher  publisher;
    @InjectMocks OrderService  orderService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(orderService, "inventoryServiceUrl", "http://inventory:8082");
    }

    private Order savedOrder(String status) {
        Order o = new Order();
        ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
        o.setCustomerId("CUST-001");
        o.setSku("SKU-001");
        o.setQuantity(5);
        o.setStatus(status);
        return o;
    }

    @Test
    @DisplayName("Accepts order when inventory is available")
    void acceptsOrderWhenInStock() {
        when(restTemplate.getForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        when(repo.save(any())).thenReturn(savedOrder("ACCEPTED"));

        OrderRequest req = new OrderRequest();
        req.setCustomerId("CUST-001");
        req.setSku("SKU-001");
        req.setQuantity(5);

        OrderResponse res = orderService.createOrder(req);

        assertThat(res.getStatus()).isEqualTo("ACCEPTED");
        verify(publisher, times(1)).publishOrderAccepted(any(), eq("SKU-001"), eq(5));
    }

    @Test
    @DisplayName("Rejects order when inventory is unavailable")
    void rejectsOrderWhenOutOfStock() {
        when(restTemplate.getForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        when(repo.save(any())).thenReturn(savedOrder("REJECTED_OUT_OF_STOCK"));

        OrderRequest req = new OrderRequest();
        req.setCustomerId("CUST-001");
        req.setSku("SKU-001");
        req.setQuantity(9999);

        OrderResponse res = orderService.createOrder(req);

        assertThat(res.getStatus()).isEqualTo("REJECTED_OUT_OF_STOCK");
        verify(publisher, never()).publishOrderAccepted(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Does not publish Kafka event for rejected orders")
    void noKafkaEventOnRejection() {
        when(restTemplate.getForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        when(repo.save(any())).thenReturn(savedOrder("REJECTED_OUT_OF_STOCK"));

        OrderRequest req = new OrderRequest();
        req.setCustomerId("CUST-002");
        req.setSku("SKU-002");
        req.setQuantity(5000);

        orderService.createOrder(req);
        verifyNoInteractions(publisher);
    }
}
