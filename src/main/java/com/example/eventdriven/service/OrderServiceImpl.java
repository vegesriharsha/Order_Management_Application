package com.example.eventdriven.service;

import com.example.eventdriven.model.Order;
import com.example.eventdriven.model.OrderItem;
import com.example.eventdriven.model.OrderStatus;
import com.example.eventdriven.model.command.CreateOrderCommand;
import com.example.eventdriven.model.event.OrderCreatedEvent;
import com.example.eventdriven.model.event.OrderStatusChangedEvent;
import com.example.eventdriven.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of OrderService
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final MessageService messageService;

    public OrderServiceImpl(OrderRepository orderRepository, MessageService messageService) {
        this.orderRepository = orderRepository;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        logger.info("Creating order for customer: {}", command.getCustomerId());

        Order order = new Order(command.getCustomerId(), command.getShippingAddress());

        command.getItems().forEach(itemDto -> {
            OrderItem item = new OrderItem(
                    itemDto.getProductId(),
                    itemDto.getProductName(),
                    itemDto.getQuantity(),
                    itemDto.getPrice()
            );
            order.addItem(item);
        });

        Order savedOrder = orderRepository.save(order);

        // Publish order created event
        publishOrderCreatedEvent(savedOrder);

        logger.info("Order created with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    @Override
    public Optional<Order> getOrderById(String orderId) {
        logger.debug("Getting order by ID: {}", orderId);
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> getOrdersByCustomerId(String customerId) {
        logger.debug("Getting orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        logger.info("Updating order status for order ID: {} to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus oldStatus = order.getStatus();

        // Check if the status transition is valid
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        order.updateStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // Publish order status changed event
        publishOrderStatusChangedEvent(savedOrder, oldStatus, newStatus);

        logger.info("Order status updated to {} for order ID: {}", newStatus, orderId);
        return savedOrder;
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderId) {
        logger.info("Cancelling order with ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus oldStatus = order.getStatus();

        // Check if the order can be cancelled
        if (!canCancel(oldStatus)) {
            throw new IllegalArgumentException(
                    "Cannot cancel order in status: " + oldStatus);
        }

        order.updateStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Publish order status changed event
        publishOrderStatusChangedEvent(savedOrder, oldStatus, OrderStatus.CANCELLED);

        logger.info("Order cancelled with ID: {}", orderId);
        return savedOrder;
    }

    /**
     * Check if the status transition is valid
     */
    private boolean isValidStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        if (oldStatus == newStatus) {
            return false;
        }

        switch (oldStatus) {
            case CREATED:
                return newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID:
                return newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING:
                return newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == OrderStatus.DELIVERED;
            case DELIVERED:
                return newStatus == OrderStatus.REFUNDED;
            case CANCELLED:
            case REFUNDED:
                return false;
            default:
                return false;
        }
    }

    /**
     * Check if an order can be cancelled based on its current status
     */
    private boolean canCancel(OrderStatus status) {
        return status == OrderStatus.CREATED ||
                status == OrderStatus.PAID ||
                status == OrderStatus.PROCESSING;
    }

    /**
     * Publish order created event
     */
    private void publishOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()))
                .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                itemDtos
        );

        messageService.sendOrderEvent(event);
        logger.info("Published OrderCreatedEvent for order ID: {}", order.getId());
    }

    /**
     * Publish order status changed event
     */
    private void publishOrderStatusChangedEvent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(),
                oldStatus,
                newStatus
        );

        messageService.sendOrderEvent(event);
        logger.info("Published OrderStatusChangedEvent for order ID: {}", order.getId());
    }
}
