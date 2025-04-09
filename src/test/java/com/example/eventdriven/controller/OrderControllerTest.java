package com.example.eventdriven.controller;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderItem;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.command.CreateOrderCommand;
import com.example.eventdriven.model.command.UpdateOrderStatusCommand;
import com.example.eventdriven.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController
 * Using a more direct approach without loading the Spring context to avoid RabbitMQ dependency issues
 */
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler(null))
                .build();
        
        // Configure object mapper
        objectMapper.findAndRegisterModules();
    }

    @Test
    void createOrder_shouldCreateOrderAndReturnCreatedStatus() throws Exception {
        // Arrange
        CreateOrderCommand command = createSampleOrderCommand();
        Order createdOrder = createSampleOrder();

        when(orderService.createOrder(any(CreateOrderCommand.class))).thenReturn(createdOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("order-123")))
                .andExpect(jsonPath("$.customerId", is(command.getCustomerId())))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    void getOrder_shouldReturnOrder() throws Exception {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();

        when(orderService.getOrderById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("order-123")))
                .andExpect(jsonPath("$.customerId", is(order.getCustomerId())))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    void getOrder_shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Arrange
        String orderId = "non-existent-order";

        when(orderService.getOrderById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByCustomer_shouldReturnOrderList() throws Exception {
        // Arrange
        String customerId = "customer-123";
        List<Order> orders = Arrays.asList(createSampleOrder(), createSampleOrder());

        when(orderService.getOrdersByCustomerId(customerId)).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId", is(customerId)))
                .andExpect(jsonPath("$[1].customerId", is(customerId)));
    }

    @Test
    void updateOrderStatus_shouldUpdateStatusAndReturnOrder() throws Exception {
        // Arrange
        String orderId = "order-123";
        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(orderId, OrderStatus.PAID);
        Order updatedOrder = createSampleOrder();
        updatedOrder.setStatus(OrderStatus.PAID);

        when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.PAID))).thenReturn(updatedOrder);

        // Act & Assert
        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("order-123")))
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    void cancelOrder_shouldCancelOrderAndReturnOrder() throws Exception {
        // Arrange
        String orderId = "order-123";
        Order cancelledOrder = createSampleOrder();
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        when(orderService.cancelOrder(orderId)).thenReturn(cancelledOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("order-123")))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    // Helper methods
    private CreateOrderCommand createSampleOrderCommand() {
        CreateOrderCommand.OrderItemDto item1 = new CreateOrderCommand.OrderItemDto(
                "product-1", "Product 1", 2, new BigDecimal("10.00"));
        CreateOrderCommand.OrderItemDto item2 = new CreateOrderCommand.OrderItemDto(
                "product-2", "Product 2", 1, new BigDecimal("15.50"));

        return new CreateOrderCommand(
                "customer-123",
                "123 Test Street, Test City",
                Arrays.asList(item1, item2)
        );
    }

    private Order createSampleOrder() {
        Order order = new Order("customer-123", "123 Test Street, Test City");
        
        // Set a fixed ID for predictable test results
        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, "order-123");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set order ID field", e);
        }
        
        // Create detached order items (to avoid circular references in JSON)
        OrderItem item1 = new OrderItem("product-1", "Product 1", 2, new BigDecimal("10.00"));
        OrderItem item2 = new OrderItem("product-2", "Product 2", 1, new BigDecimal("15.50"));
        
        // Add the items in a way that doesn't create circular references
        try {
            java.lang.reflect.Field itemsField = Order.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            List<OrderItem> items = new ArrayList<>();
            items.add(item1);
            items.add(item2);
            itemsField.set(order, items);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set order items field", e);
        }
        
        return order;
    }
}