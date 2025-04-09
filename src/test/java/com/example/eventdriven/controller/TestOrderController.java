package com.example.eventdriven.controller;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.SerializableOrder;
import com.example.eventdriven.model.command.CreateOrderCommand;
import com.example.eventdriven.model.command.UpdateOrderStatusCommand;
import com.example.eventdriven.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A test version of the OrderController that uses SerializableOrder 
 * to avoid circular reference issues in tests
 */
@RestController
@RequestMapping("/api/orders")
public class TestOrderController {
    
    private final OrderService orderService;
    
    public TestOrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<SerializableOrder> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        Order order = orderService.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SerializableOrder(order));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<SerializableOrder> getOrder(@PathVariable String orderId) {
        return orderService.getOrderById(orderId)
                .map(SerializableOrder::new)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SerializableOrder>> getOrdersByCustomer(@PathVariable String customerId) {
        List<SerializableOrder> orders = orderService.getOrdersByCustomerId(customerId)
                .stream()
                .map(SerializableOrder::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<SerializableOrder> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusCommand command) {
        
        if (!orderId.equals(command.getOrderId())) {
            throw new RuntimeException("Order ID mismatch");
        }
        
        Order order = orderService.updateOrderStatus(orderId, command.getNewStatus());
        return ResponseEntity.ok(new SerializableOrder(order));
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<SerializableOrder> cancelOrder(@PathVariable String orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(new SerializableOrder(order));
    }
}