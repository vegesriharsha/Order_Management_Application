package com.example.eventdriven.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Jackson mixin for OrderItem to handle circular references
 */
public abstract class OrderItemMixin {
    
    @JsonBackReference
    abstract Order getOrder();
}