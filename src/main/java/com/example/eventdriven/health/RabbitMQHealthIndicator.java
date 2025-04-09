package com.example.eventdriven.health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for RabbitMQ connection
 */
@Component
public class RabbitMQHealthIndicator extends AbstractHealthIndicator {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQHealthIndicator(RabbitTemplate rabbitTemplate) {
        super("RabbitMQ health check failed");
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Check if RabbitMQ connection is available
            rabbitTemplate.execute(channel -> {
                // If we can execute this lambda, the connection is working
                return channel.isOpen();
            });

            builder.up()
                    .withDetail("status", "Available")
                    .withDetail("connectionFactory",
                            rabbitTemplate.getConnectionFactory().getClass().getSimpleName());
        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Unavailable")
                    .withDetail("error", e.getMessage());
        }
    }
}
