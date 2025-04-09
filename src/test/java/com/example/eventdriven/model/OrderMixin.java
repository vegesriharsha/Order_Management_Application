package com.example.eventdriven.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.List;

/**
 * Jackson mixin for Order to handle circular references
 */
public abstract class OrderMixin {
    
    @JsonManagedReference
    abstract List<OrderItem> getItems();
}