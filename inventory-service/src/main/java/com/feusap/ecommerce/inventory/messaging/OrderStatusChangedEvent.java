package com.feusap.ecommerce.inventory.messaging;

/** Local view of order.confirmed / order.cancelled: only the order id is needed. */
public record OrderStatusChangedEvent(Long orderId, String status) {
}
