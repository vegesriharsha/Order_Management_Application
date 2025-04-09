package com.example.eventdriven.model.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all event messages in the system.
 * Provides common properties for tracing and tracking events.
 */
public abstract class EventMessage<T> {
    private String messageId;
    private String correlationId;
    private LocalDateTime timestamp;
    private T payload;
    private String eventType;
    private String source;
    private int version;

    protected EventMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.version = 1;
    }

    protected EventMessage(T payload, String eventType, String source) {
        this();
        this.payload = payload;
        this.eventType = eventType;
        this.source = source;
        this.correlationId = this.messageId;
    }

    protected EventMessage(T payload, String eventType, String source, String correlationId) {
        this(payload, eventType, source);
        if (correlationId != null && !correlationId.isBlank()) {
            this.correlationId = correlationId;
        }
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
