package com.feusap.ecommerce.inventory.messaging;

import java.util.List;

/**
 * Local view of the order.created event. Only the fields this service needs are declared;
 * unknown fields published by the order service are ignored.
 */
public record OrderCreatedEvent(Long orderId, List<OrderedItem> items) {

    public record OrderedItem(String productId, int quantity) {
    }
}
