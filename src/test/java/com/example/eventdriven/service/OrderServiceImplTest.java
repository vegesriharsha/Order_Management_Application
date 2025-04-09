package com.example.eventdriven.service;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderItem;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.command.CreateOrderCommand;
import com.example.eventdriven.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MessageService messageService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, messageService);
    }

    @Test
    void createOrder_shouldSaveOrderAndPublishEvent() {
        // Arrange
        CreateOrderCommand command = createSampleOrderCommand();

        Order savedOrder = new Order(command.getCustomerId(), command.getShippingAddress());
        command.getItems().forEach(itemDto ->
                savedOrder.addItem(new OrderItem(
                        itemDto.getProductId(),
                        itemDto.getProductName(),
                        itemDto.getQuantity(),
                        itemDto.getPrice()
                ))
        );

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        Order result = orderService.createOrder(command);

        // Assert
        assertNotNull(result);
        assertEquals(command.getCustomerId(), result.getCustomerId());
        assertEquals(command.getShippingAddress(), result.getShippingAddress());
        assertEquals(OrderStatus.CREATED, result.getStatus());

        verify(orderRepository).save(any(Order.class));
        verify(messageService).sendOrderEvent(any());
    }

    @Test
    void getOrderById_shouldReturnOrder() {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        Optional<Order> result = orderService.getOrderById(orderId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(order, result.get());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrdersByCustomerId_shouldReturnCustomerOrders() {
        // Arrange
        String customerId = "customer-123";
        List<Order> orders = Arrays.asList(createSampleOrder(), createSampleOrder());
        when(orderRepository.findByCustomerId(customerId)).thenReturn(orders);

        // Act
        List<Order> result = orderService.getOrdersByCustomerId(customerId);

        // Assert
        assertEquals(2, result.size());
        verify(orderRepository).findByCustomerId(customerId);
    }

    @Test
    void updateOrderStatus_shouldUpdateStatusAndPublishEvent() {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        // Assert
        assertEquals(OrderStatus.PAID, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(messageService).sendOrderEvent(any());
    }

    @Test
    void updateOrderStatus_shouldThrowExceptionForInvalidTransition() {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED));

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(messageService, never()).sendOrderEvent(any());
    }

    @Test
    void updateOrderStatus_shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        String orderId = "order-123";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.updateOrderStatus(orderId, OrderStatus.PAID));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldCancelOrder() {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = orderService.cancelOrder(orderId);

        // Assert
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(messageService).sendOrderEvent(any());
    }

    @Test
    void cancelOrder_shouldThrowExceptionWhenOrderCannotBeCancelled() {
        // Arrange
        String orderId = "order-123";
        Order order = createSampleOrder();
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.cancelOrder(orderId));

        assertTrue(exception.getMessage().contains("Cannot cancel order"));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(messageService, never()).sendOrderEvent(any());
    }

    @Test
    void cancelOrder_shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        String orderId = "order-123";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.cancelOrder(orderId));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
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
        order.addItem(new OrderItem("product-1", "Product 1", 2, new BigDecimal("10.00")));
        order.addItem(new OrderItem("product-2", "Product 2", 1, new BigDecimal("15.50")));
        return order;
    }
}
