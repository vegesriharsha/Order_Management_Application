package com.example.eventdriven.service;

import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.event.OrderCreatedEvent;
import com.example.eventdriven.model.event.OrderStatusChangedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final String COMMAND_EXCHANGE = "service.command";
    private static final String EVENT_EXCHANGE = "service.event";
    private static final String BROADCAST_EXCHANGE = "service.broadcast";
    private static final String ORDER_COMMAND_ROUTING_KEY = "orders.command";
    private static final String ORDER_EVENT_ROUTING_KEY = "orders.event";
    private static final String ORDER_BROADCAST_ROUTING_KEY = "orders.broadcast";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(rabbitTemplate);

        // Set properties via reflection since we're not loading the application context
        ReflectionTestUtils.setField(messageService, "commandExchange", COMMAND_EXCHANGE);
        ReflectionTestUtils.setField(messageService, "eventExchange", EVENT_EXCHANGE);
        ReflectionTestUtils.setField(messageService, "broadcastExchange", BROADCAST_EXCHANGE);
        ReflectionTestUtils.setField(messageService, "orderCommandRoutingKey", ORDER_COMMAND_ROUTING_KEY);
        ReflectionTestUtils.setField(messageService, "orderEventRoutingKey", ORDER_EVENT_ROUTING_KEY);
        ReflectionTestUtils.setField(messageService, "orderBroadcastRoutingKey", ORDER_BROADCAST_ROUTING_KEY);
    }

    @Test
    void sendCommand_shouldSendToCommandExchange() {
        // Arrange
        Object command = new Object();
        String routingKey = "test.routing.key";

        // Act
        messageService.sendCommand(command, routingKey);

        // Assert
        verify(rabbitTemplate).convertAndSend(COMMAND_EXCHANGE, routingKey, command);
    }

    @Test
    void sendEvent_shouldSendToEventExchange() {
        // Arrange
        Object event = new Object();
        String routingKey = "test.routing.key";

        // Act
        messageService.sendEvent(event, routingKey);

        // Assert
        verify(rabbitTemplate).convertAndSend(EVENT_EXCHANGE, routingKey, event);
    }

    @Test
    void broadcast_shouldSendToBroadcastExchange() {
        // Arrange
        Object message = new Object();

        // Act
        messageService.broadcast(message);

        // Assert
        verify(rabbitTemplate).convertAndSend(BROADCAST_EXCHANGE, "", message);
    }

    @Test
    void sendOrderCommand_shouldSendToCommandExchangeWithOrderRoutingKey() {
        // Arrange
        Object command = new Object();

        // Act
        messageService.sendOrderCommand(command);

        // Assert
        verify(rabbitTemplate).convertAndSend(COMMAND_EXCHANGE, ORDER_COMMAND_ROUTING_KEY, command);
    }

    @Test
    void sendOrderEvent_shouldSendToEventExchangeWithOrderRoutingKey() {
        // Arrange
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-123", 
            "customer-456", 
            OrderStatus.CREATED, 
            LocalDateTime.now(), 
            new BigDecimal("100.00"), 
            "123 Test St", 
            new ArrayList<>()
        );

        // Act
        messageService.sendOrderEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(EVENT_EXCHANGE, ORDER_EVENT_ROUTING_KEY, event);
    }

    @Test
    void broadcastOrderMessage_shouldSendToBroadcastExchange() {
        // Arrange
        OrderStatusChangedEvent message = new OrderStatusChangedEvent(
            "order-123", 
            OrderStatus.CREATED, 
            OrderStatus.PAID
        );

        // Act
        messageService.broadcastOrderMessage(message);

        // Assert
        verify(rabbitTemplate).convertAndSend(BROADCAST_EXCHANGE, "", message);
    }
}
