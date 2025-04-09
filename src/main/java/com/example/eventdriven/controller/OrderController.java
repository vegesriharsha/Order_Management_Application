package com.example.eventdriven.controller;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.command.CreateOrderCommand;
import com.example.eventdriven.model.command.UpdateOrderStatusCommand;
import com.example.eventdriven.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for order operations
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order
     *
     * @param command the order creation command
     * @return the created order
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        logger.info("Received request to create order for customer: {}", command.getCustomerId());
        Order order = orderService.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get order by ID
     *
     * @param orderId the order ID
     * @return the order if found
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        logger.info("Received request to get order with ID: {}", orderId);
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    /**
     * Get orders by customer ID
     *
     * @param customerId the customer ID
     * @return list of orders
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        logger.info("Received request to get orders for customer: {}", customerId);
        List<Order> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order status
     *
     * @param orderId the order ID
     * @param command the status update command
     * @return the updated order
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusCommand command) {

        logger.info("Received request to update status for order: {} to {}",
                orderId, command.getNewStatus());

        if (!orderId.equals(command.getOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order ID in path and body must match");
        }

        try {
            Order order = orderService.updateOrderStatus(orderId, command.getNewStatus());
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Cancel an order
     *
     * @param orderId the order ID
     * @return the cancelled order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String orderId) {
        logger.info("Received request to cancel order: {}", orderId);

        try {
            Order order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
