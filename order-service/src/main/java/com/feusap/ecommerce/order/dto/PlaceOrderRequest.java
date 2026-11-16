package com.feusap.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Temporary contract: once authentication is in place, the customer will be read from the JWT
 * and this payload will disappear.
 */
public record PlaceOrderRequest(@NotNull @Positive Long customerId) {
}
