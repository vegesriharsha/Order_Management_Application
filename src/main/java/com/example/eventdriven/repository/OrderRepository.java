package com.example.eventdriven.repository;

import com.example.eventdriven.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entity operations
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Find all orders for a specific customer
     *
     * @param customerId the customer ID
     * @return list of orders for the customer
     */
    List<Order> findByCustomerId(String customerId);
}
