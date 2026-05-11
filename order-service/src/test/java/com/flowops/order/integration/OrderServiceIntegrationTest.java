package com.flowops.order.integration;

import com.flowops.order.dto.OrderRequest;
import com.flowops.order.dto.OrderResponse;
import com.flowops.order.repository.OrderRepository;
import com.flowops.order.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order.accepted"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:ordertest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "inventory.service.url=http://inventory-mock"
})
@DirtiesContext
class OrderServiceIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private RestTemplate restTemplate;
    @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Accepted order is persisted and Kafka event published to order.accepted")
    void acceptedOrder_persistedAndKafkaEventPublished() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo(containsString("/api/inventory/check")))
                  .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, String> container =
            buildContainer("test-accept", "order.accepted", records);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        OrderResponse response = orderService.createOrder(request("CUST-001", "SKU-001", 10));

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(orderRepository.findAll()).hasSize(1);

        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("SKU-001");

        container.stop();
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejected order is persisted and no Kafka event is published")
    void rejectedOrder_persistedNoKafkaEvent() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo(containsString("/api/inventory/check")))
                  .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));

        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, String> container =
            buildContainer("test-reject", "order.accepted", records);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        OrderResponse response = orderService.createOrder(request("CUST-002", "SKU-002", 9999));

        assertThat(response.getStatus()).isEqualTo("REJECTED_OUT_OF_STOCK");
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(records.poll(2, TimeUnit.SECONDS)).isNull();

        container.stop();
        mockServer.verify();
    }

    @Test
    @DisplayName("Circuit breaker fallback saves order as PENDING_INVENTORY_CHECK")
    void circuitBreakerFallback_savesPendingOrder() {
        OrderResponse response = orderService.fallbackCreateOrder(
            request("CUST-003", "SKU-001", 5), new RuntimeException("Service down"));

        assertThat(response.getStatus()).isEqualTo("PENDING_INVENTORY_CHECK");
        assertThat(orderRepository.findAll()).hasSize(1);
    }

    private KafkaMessageListenerContainer<String, String> buildContainer(
            String groupId, String topic, BlockingQueue<ConsumerRecord<String, String>> queue) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(props);
        KafkaMessageListenerContainer<String, String> container =
            new KafkaMessageListenerContainer<>(cf, new ContainerProperties(topic));
        container.setupMessageListener((MessageListener<String, String>) queue::add);
        return container;
    }

    private OrderRequest request(String customerId, String sku, int qty) {
        OrderRequest req = new OrderRequest();
        req.setCustomerId(customerId);
        req.setSku(sku);
        req.setQuantity(qty);
        return req;
    }
}
