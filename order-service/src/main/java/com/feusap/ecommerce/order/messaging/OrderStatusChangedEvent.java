package com.feusap.ecommerce.order.messaging;

import com.feusap.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderStatusChangedEvent(
    Long orderId,
    Long customerId,
    String status,
    BigDecimal totalAmount,
    Instant occurredAt,
    List<OrderedItem> items
) {
    public record OrderedItem(String productId, String productName, int quantity) {
    }

    public static OrderStatusChangedEvent from(Order order) {
        return new OrderStatusChangedEvent(order.getId(), order.getCustomerId(),
            order.getStatus().name(), order.getTotalAmount(), Instant.now(),
            order.getLines().stream()
                .map(l -> new OrderedItem(l.getProductId(), l.getProductName(), l.getQuantity()))
                .toList());
    }
}
