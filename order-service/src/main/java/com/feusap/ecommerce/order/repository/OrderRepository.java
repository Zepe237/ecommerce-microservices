package com.feusap.ecommerce.order.repository;

import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesById(Long id);

    @EntityGraph(attributePaths = "lines")
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    @EntityGraph(attributePaths = "lines")
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant cutoff);
}
