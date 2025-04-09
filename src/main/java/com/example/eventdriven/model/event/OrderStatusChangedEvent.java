package com.example.eventdriven.model.event;

import com.example.eventdriven.model.OrderStatus;

import java.time.LocalDateTime;

public class OrderStatusChangedEvent {
    private String orderId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private LocalDateTime timestamp;

    // Default constructor for JSON deserialization
    public OrderStatusChangedEvent() {
    }

    public OrderStatusChangedEvent(String orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(OrderStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OrderStatusChangedEvent{" +
                "orderId='" + orderId + '\'' +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", timestamp=" + timestamp +
                '}';
    }
}
