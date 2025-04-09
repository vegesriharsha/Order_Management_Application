package com.example.eventdriven.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending messages to RabbitMQ
 */
@Service
public class MessageService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchanges.command}")
    private String commandExchange;

    @Value("${app.rabbitmq.exchanges.event}")
    private String eventExchange;

    @Value("${app.rabbitmq.exchanges.broadcast}")
    private String broadcastExchange;

    @Value("${app.rabbitmq.routing-keys.orders.command}")
    private String orderCommandRoutingKey;

    @Value("${app.rabbitmq.routing-keys.orders.event}")
    private String orderEventRoutingKey;

    @Value("${app.rabbitmq.routing-keys.orders.broadcast}")
    private String orderBroadcastRoutingKey;

    public MessageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Send a command message
     *
     * @param command the command to send
     * @param routingKey the routing key to use
     * @param <T> the type of the command
     */
    public <T> void sendCommand(T command, String routingKey) {
        rabbitTemplate.convertAndSend(commandExchange, routingKey, command);
    }

    /**
     * Send an event message
     *
     * @param event the event to send
     * @param routingKey the routing key to use
     * @param <T> the type of the event
     */
    public <T> void sendEvent(T event, String routingKey) {
        rabbitTemplate.convertAndSend(eventExchange, routingKey, event);
    }

    /**
     * Broadcast a message to all subscribers
     *
     * @param message the message to broadcast
     * @param <T> the type of the message
     */
    public <T> void broadcast(T message) {
        rabbitTemplate.convertAndSend(broadcastExchange, "", message);
    }

    /**
     * Send an order command
     *
     * @param command the order command to send
     * @param <T> the type of the command
     */
    public <T> void sendOrderCommand(T command) {
        sendCommand(command, orderCommandRoutingKey);
    }

    /**
     * Send an order event
     *
     * @param event the order event to send
     * @param <T> the type of the event
     */
    public <T> void sendOrderEvent(T event) {
        sendEvent(event, orderEventRoutingKey);
    }

    /**
     * Broadcast an order message
     *
     * @param message the order message to broadcast
     * @param <T> the type of the message
     */
    public <T> void broadcastOrderMessage(T message) {
        broadcast(message);
    }
}
