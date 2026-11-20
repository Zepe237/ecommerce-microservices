package com.feusap.ecommerce.cart.messaging;

/**
 * Local view of the order.confirmed event: the cart service only needs to know whose cart to empty.
 */
public record OrderStatusChangedEvent(Long orderId, Long customerId) {
}
