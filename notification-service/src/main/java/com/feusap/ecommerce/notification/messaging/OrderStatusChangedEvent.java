package com.feusap.ecommerce.notification.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderStatusChangedEvent(
    Long orderId,
    Long customerId,
    BigDecimal totalAmount,
    Instant occurredAt,
    List<OrderedItem> items
) {

    public record OrderedItem(String productId, String productName, int quantity) {
    }
}
