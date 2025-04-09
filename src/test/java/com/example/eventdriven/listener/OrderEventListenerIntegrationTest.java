package com.example.eventdriven.listener;

import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.event.OrderCreatedEvent;
import com.example.eventdriven.model.event.OrderStatusChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
@org.junit.jupiter.api.Disabled("Requires Docker to run - enable when Docker is available")
class OrderEventListenerIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.9.11-management"))
            .withExposedPorts(5672, 15672);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private OrderEventListener orderEventListener;

    @Value("${app.rabbitmq.exchanges.event}")
    private String eventExchange;

    @Value("${app.rabbitmq.routing-keys.orders.event}")
    private String orderEventRoutingKey;

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    }

    @BeforeEach
    void setUp() {
        // Wait for RabbitMQ to be fully initialized
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        rabbitTemplate.execute(channel -> true);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    @Test
    void handleOrderCreatedEvent_shouldProcessEvent() throws Exception {
        // Arrange
        OrderCreatedEvent event = createSampleOrderCreatedEvent();

        // Act
        rabbitTemplate.convertAndSend(eventExchange, orderEventRoutingKey, event);

        // Assert
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orderEventListener, times(1))
                        .handleOrderCreatedEvent(any(OrderCreatedEvent.class), any(), any()));
    }

    @Test
    void handleOrderStatusChangedEvent_shouldProcessEvent() throws Exception {
        // Arrange
        OrderStatusChangedEvent event = createSampleOrderStatusChangedEvent();

        // Act
        rabbitTemplate.convertAndSend(eventExchange, orderEventRoutingKey, event);

        // Assert
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orderEventListener, times(1))
                        .handleOrderStatusChangedEvent(any(OrderStatusChangedEvent.class), any(), any()));
    }

    // Helper methods
    private OrderCreatedEvent createSampleOrderCreatedEvent() {
        OrderCreatedEvent.OrderItemDto item1 = new OrderCreatedEvent.OrderItemDto(
                "product-1", "Product 1", 2, new BigDecimal("10.00"));
        OrderCreatedEvent.OrderItemDto item2 = new OrderCreatedEvent.OrderItemDto(
                "product-2", "Product 2", 1, new BigDecimal("15.50"));

        return new OrderCreatedEvent(
                "order-123",
                "customer-123",
                OrderStatus.CREATED,
                LocalDateTime.now(),
                new BigDecimal("35.50"),
                "123 Test Street, Test City",
                Arrays.asList(item1, item2)
        );
    }

    private OrderStatusChangedEvent createSampleOrderStatusChangedEvent() {
        return new OrderStatusChangedEvent(
                "order-123",
                OrderStatus.CREATED,
                OrderStatus.PAID
        );
    }
}
