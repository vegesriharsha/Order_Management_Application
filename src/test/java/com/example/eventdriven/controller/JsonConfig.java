package com.example.eventdriven.controller;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for handling proper JSON serialization of entity relationships
 */
@Configuration
public class JsonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Custom module to handle circular references
        SimpleModule module = new SimpleModule();
        
        // Configure JSON mixins to break circular references
        objectMapper.addMixIn(OrderItem.class, OrderItemMixin.class);
        
        return objectMapper;
    }
    
    /**
     * Mixin to handle circular references in OrderItem
     */
    abstract class OrderItemMixin {
        @JsonIgnore
        abstract Order getOrder();
    }
}