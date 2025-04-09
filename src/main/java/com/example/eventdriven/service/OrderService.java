package com.example.eventdriven.service;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.command.CreateOrderCommand;

import java.util.List;
import java.util.Optional;

/**
 * Service for order management operations
 */
public interface OrderService {

    /**
     * Create a new order
     *
     * @param command the command containing order details
     * @return the created order
     */
    Order createOrder(CreateOrderCommand command);

    /**
     * Get order by ID
     *
     * @param orderId the order ID
     * @return optional containing the order if found
     */
    Optional<Order> getOrderById(String orderId);

    /**
     * Get all orders for a customer
     *
     * @param customerId the customer ID
     * @return list of orders for the customer
     */
    List<Order> getOrdersByCustomerId(String customerId);

    /**
     * Update the status of an order
     *
     * @param orderId the order ID
     * @param newStatus the new status
     * @return the updated order
     * @throws IllegalArgumentException if the order is not found
     */
    Order updateOrderStatus(String orderId, OrderStatus newStatus);

    /**
     * Cancel an order
     *
     * @param orderId the order ID
     * @return the cancelled order
     * @throws IllegalArgumentException if the order is not found or cannot be cancelled
     */
    Order cancelOrder(String orderId);
}
