package com.feusap.ecommerce.order.messaging;



import com.feusap.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderStatusChangedEvent(
    Long orderId,
    Long customerId,
    String status,          // "CONFIRMED" ou "CANCELLED"
    BigDecimal totalAmount,
    Instant occurredAt
) {
    public static OrderStatusChangedEvent from(Order order) {
        return new OrderStatusChangedEvent(order.getId(), order.getCustomerId(),
            order.getStatus().name(), order.getTotalAmount(), Instant.now());
    }
}
