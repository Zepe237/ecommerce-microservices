package com.feusap.ecommerce.notification.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Local view of the order.created event published by the order service.
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
}
