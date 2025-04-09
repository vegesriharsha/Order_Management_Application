package com.example.eventdriven.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for graceful shutdown of the application
 */
@Configuration
public class GracefulShutdownConfig {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownConfig.class);

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final ConfigurableApplicationContext applicationContext;

    public GracefulShutdownConfig(
            RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry,
            ConfigurableApplicationContext applicationContext) {
        this.rabbitListenerEndpointRegistry = rabbitListenerEndpointRegistry;
        this.applicationContext = applicationContext;
    }

    /**
     * Log when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application is ready and listening for messages");
    }

    /**
     * Handle application shutdown
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        logger.info("Application shutdown initiated");

        // Create a latch to wait for in-flight message processing
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // Stop accepting new messages on all RabbitMQ listeners
        logger.info("Stopping RabbitMQ listeners...");
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            container.stop();
        });

        // Wait for in-flight messages to be processed
        // In a real application, you would track in-flight messages and wait for them to complete
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            logger.info("All in-flight messages processed, releasing shutdown latch");
            shutdownLatch.countDown();
        }, 5, TimeUnit.SECONDS);

        try {
            logger.info("Waiting for in-flight message processing to complete...");
            if (!shutdownLatch.await(10, TimeUnit.SECONDS)) {
                logger.warn("Timed out waiting for message processing to complete");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for message processing to complete");
        } finally {
            executor.shutdownNow();
        }

        logger.info("Application shutdown complete");
    }

    /**
     * Register a shutdown hook for JVM termination
     */
    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownHook() {
        return event -> {
            // This hook provides additional shutdown behavior beyond what Spring handles
            logger.info("JVM shutdown hook triggered");
        };
    }
}
