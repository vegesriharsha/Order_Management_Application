package com.example.eventdriven.listener;

import com.example.eventdriven.model.event.OrderCreatedEvent;
import com.example.eventdriven.model.event.OrderStatusChangedEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Listener for order-related events from RabbitMQ queues
 */
@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    /**
     * Listen for order created events
     *
     * @param event the event
     * @param channel the RabbitMQ channel
     * @param deliveryTag the delivery tag
     * @throws IOException if there's an issue with acknowledging the message
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.orders.event}")
    @Retryable(maxAttempts = 3)
    public void handleOrderCreatedEvent(
            OrderCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        try {
            logger.info("Received OrderCreatedEvent for order ID: {}", event.getOrderId());

            // Process the event - in a real application, this might trigger
            // notifications, inventory updates, etc.
            processOrderCreated(event);

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);

            logger.info("Successfully processed OrderCreatedEvent for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            logger.error("Error processing OrderCreatedEvent for order ID: {}", event.getOrderId(), e);

            // Reject the message and don't requeue (it will go to the DLQ)
            channel.basicReject(deliveryTag, false);
        }
    }

    /**
     * Listen for order status changed events
     *
     * @param event the event
     * @param channel the RabbitMQ channel
     * @param deliveryTag the delivery tag
     * @throws IOException if there's an issue with acknowledging the message
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.orders.event}")
    @Retryable(maxAttempts = 3)
    public void handleOrderStatusChangedEvent(
            OrderStatusChangedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        try {
            logger.info("Received OrderStatusChangedEvent for order ID: {}, new status: {}",
                    event.getOrderId(), event.getNewStatus());

            // Process the event - in a real application, this might trigger
            // notifications, shipping requests, etc.
            processOrderStatusChanged(event);

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);

            logger.info("Successfully processed OrderStatusChangedEvent for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            logger.error("Error processing OrderStatusChangedEvent for order ID: {}", event.getOrderId(), e);

            // Reject the message and don't requeue (it will go to the DLQ)
            channel.basicReject(deliveryTag, false);
        }
    }

    /**
     * Process order created event
     *
     * @param event the event
     */
    private void processOrderCreated(OrderCreatedEvent event) {
        // In a real application, this would contain business logic
        // For example:
        // - Send notification to customer
        // - Update inventory
        // - Trigger payment processing
        // etc.

        logger.debug("Processing order created: {}", event.getOrderId());

        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process order status changed event
     *
     * @param event the event
     */
    private void processOrderStatusChanged(OrderStatusChangedEvent event) {
        // In a real application, this would contain business logic
        // For example:
        // - Send notification to customer about status change
        // - If PAID, trigger fulfillment process
        // - If SHIPPED, generate shipping notification
        // etc.

        logger.debug("Processing order status changed: {} -> {}",
                event.getOrderId(), event.getNewStatus());

        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
