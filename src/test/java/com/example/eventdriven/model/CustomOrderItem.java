package com.example.eventdriven.model;

import java.math.BigDecimal;

/**
 * A simplified version of OrderItem for testing purposes
 * This breaks the circular reference that causes JSON serialization issues
 */
public class CustomOrderItem {
    private String id;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    
    public CustomOrderItem(OrderItem item) {
        this.id = item.getId();
        this.productId = item.getProductId();
        this.productName = item.getProductName();
        this.quantity = item.getQuantity();
        this.price = item.getPrice();
    }
    
    public String getId() {
        return id;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
}