package com.example.eventdriven.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A serialization-friendly version of Order for testing purposes
 * This breaks the circular reference that causes JSON serialization issues
 */
public class SerializableOrder {
    private String id;
    private String customerId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<CustomOrderItem> items;
    
    public SerializableOrder(Order order) {
        this.id = order.getId();
        this.customerId = order.getCustomerId();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.totalAmount = order.getTotalAmount();
        this.shippingAddress = order.getShippingAddress();
        
        // Convert OrderItems to CustomOrderItems to break circular reference
        this.items = order.getItems().stream()
                .map(CustomOrderItem::new)
                .collect(Collectors.toList());
    }
    
    public String getId() {
        return id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public List<CustomOrderItem> getItems() {
        return items;
    }
}