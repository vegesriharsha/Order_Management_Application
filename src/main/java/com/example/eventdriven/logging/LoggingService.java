package com.example.eventdriven.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for centralized logging through RabbitMQ
 */
@Service
public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String applicationName;

    @Value("${app.rabbitmq.exchanges.logging}")
    private String loggingExchange;

    public LoggingService(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name}") String applicationName) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.applicationName = applicationName;
    }

    /**
     * Log an info message to the centralized logging system
     *
     * @param message the log message
     * @param data additional data to include in the log
     */
    public void logInfo(String message, Map<String, Object> data) {
        publishLog("INFO", message, data);
    }

    /**
     * Log a warning message to the centralized logging system
     *
     * @param message the log message
     * @param data additional data to include in the log
     */
    public void logWarning(String message, Map<String, Object> data) {
        publishLog("WARNING", message, data);
    }

    /**
     * Log an error message to the centralized logging system
     *
     * @param message the log message
     * @param data additional data to include in the log
     * @param throwable the exception to log
     */
    public void logError(String message, Map<String, Object> data, Throwable throwable) {
        if (data == null) {
            data = new HashMap<>();
        }

        if (throwable != null) {
            data.put("exception", throwable.getClass().getName());
            data.put("exceptionMessage", throwable.getMessage());
        }

        publishLog("ERROR", message, data);
    }

    /**
     * Publish a log message to RabbitMQ
     *
     * @param level the log level
     * @param message the log message
     * @param data additional data to include in the log
     */
    private void publishLog(String level, String message, Map<String, Object> data) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("id", UUID.randomUUID().toString());
            logEntry.put("timestamp", LocalDateTime.now().toString());
            logEntry.put("level", level);
            logEntry.put("message", message);
            logEntry.put("service", applicationName);

            if (data != null) {
                logEntry.put("data", data);
            }

            // Use routing key pattern: level.service
            String routingKey = level.toLowerCase() + "." + applicationName.toLowerCase();

            rabbitTemplate.convertAndSend(loggingExchange, routingKey, objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            // Fallback to local logging if publishing fails
            logger.error("Failed to publish log message to RabbitMQ", e);
            logger.info("Log message that failed to publish: {} - {}", level, message);
        }
    }
}
