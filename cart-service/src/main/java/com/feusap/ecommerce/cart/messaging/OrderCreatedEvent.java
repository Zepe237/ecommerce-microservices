package com.feusap.ecommerce.cart.messaging;

/**
 * Local view of the order.created event: the cart service only needs to know whose cart to empty.
 */
public record OrderCreatedEvent(Long orderId, Long customerId) {
}
