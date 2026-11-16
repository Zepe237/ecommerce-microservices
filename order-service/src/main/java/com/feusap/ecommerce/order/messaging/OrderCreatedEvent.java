package com.feusap.ecommerce.order.messaging;

import com.feusap.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Published on the order.created topic once an order has been persisted.
 * Consumers (inventory, cart, notification) each keep their own copy of this contract.
 */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderedItem> items
) {

    public record OrderedItem(String productId, String productName, int quantity) {
    }

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getLines().stream()
                        .map(line -> new OrderedItem(line.getProductId(), line.getProductName(), line.getQuantity()))
                        .toList());
    }
}
