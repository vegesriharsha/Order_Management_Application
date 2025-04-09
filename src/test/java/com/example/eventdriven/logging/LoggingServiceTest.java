package com.example.eventdriven.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingServiceTest {

    private static final String LOGGING_EXCHANGE = "service.logging";
    private static final String APPLICATION_NAME = "test-service";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;
    private LoggingService loggingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        loggingService = new LoggingService(rabbitTemplate, objectMapper, APPLICATION_NAME);

        // Set properties via reflection since we're not loading the application context
        ReflectionTestUtils.setField(loggingService, "loggingExchange", LOGGING_EXCHANGE);
    }

    @Test
    void logInfo_shouldPublishInfoLogMessage() {
        // Arrange
        String message = "Test info message";
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);

        // Act
        loggingService.logInfo(message, data);

        // Assert
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        String routingKey = routingKeyCaptor.getValue();
        String logJson = messageCaptor.getValue();

        assertTrue(routingKey.startsWith("info."));
        assertTrue(logJson.contains("\"level\":\"INFO\""));
        assertTrue(logJson.contains("\"message\":\"Test info message\""));
        assertTrue(logJson.contains("\"service\":\"test-service\""));
        assertTrue(logJson.contains("\"key1\":\"value1\""));
        assertTrue(logJson.contains("\"key2\":123"));
    }

    @Test
    void logWarning_shouldPublishWarningLogMessage() {
        // Arrange
        String message = "Test warning message";
        Map<String, Object> data = new HashMap<>();
        data.put("warning", true);

        // Act
        loggingService.logWarning(message, data);

        // Assert
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        String routingKey = routingKeyCaptor.getValue();
        String logJson = messageCaptor.getValue();

        assertTrue(routingKey.startsWith("warning."));
        assertTrue(logJson.contains("\"level\":\"WARNING\""));
        assertTrue(logJson.contains("\"message\":\"Test warning message\""));
        assertTrue(logJson.contains("\"warning\":true"));
    }

    @Test
    void logError_shouldPublishErrorLogMessageWithException() {
        // Arrange
        String message = "Test error message";
        Map<String, Object> data = new HashMap<>();
        Exception exception = new RuntimeException("Test exception");

        // Act
        loggingService.logError(message, data, exception);

        // Assert
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        String routingKey = routingKeyCaptor.getValue();
        String logJson = messageCaptor.getValue();

        assertTrue(routingKey.startsWith("error."));
        assertTrue(logJson.contains("\"level\":\"ERROR\""));
        assertTrue(logJson.contains("\"message\":\"Test error message\""));
        assertTrue(logJson.contains("\"exception\":\"java.lang.RuntimeException\""));
        assertTrue(logJson.contains("\"exceptionMessage\":\"Test exception\""));
    }

    @Test
    void logError_shouldHandleNullData() {
        // Arrange
        String message = "Test error with null data";
        Exception exception = new RuntimeException("Test exception");

        // Act
        loggingService.logError(message, null, exception);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                anyString(),
                anyString()
        );
    }

    @Test
    void logError_shouldHandleNullException() {
        // Arrange
        String message = "Test error with null exception";
        Map<String, Object> data = new HashMap<>();

        // Act
        loggingService.logError(message, data, null);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                anyString(),
                anyString()
        );
    }

    @Test
    void publishLog_shouldHandleExceptionGracefully() {
        // Arrange
        String message = "Test message that will fail";
        Map<String, Object> data = new HashMap<>();

        // Make the RabbitTemplate throw an exception
        doThrow(new RuntimeException("Test failure"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act - This should not throw an exception
        loggingService.logInfo(message, data);

        // Assert - Verify that rabbitTemplate was called
        verify(rabbitTemplate).convertAndSend(
                eq(LOGGING_EXCHANGE),
                anyString(),
                anyString()
        );
        // If we get here, the test passed because no exception was propagated
    }
}
