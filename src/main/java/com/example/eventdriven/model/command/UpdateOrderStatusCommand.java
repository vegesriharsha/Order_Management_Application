package com.example.eventdriven.model.command;

import com.example.eventdriven.model.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command to update an order's status
 */
public class UpdateOrderStatusCommand {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    // Default constructor for JSON deserialization
    public UpdateOrderStatusCommand() {
    }

    public UpdateOrderStatusCommand(String orderId, OrderStatus newStatus) {
        this.orderId = orderId;
        this.newStatus = newStatus;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }
}
