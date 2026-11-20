package com.feusap.ecommerce.inventory.exception;

import lombok.Getter;

import java.util.List;

/**
 * The stock of at least one product is not enough to reserve an order. Nothing is reserved.
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<String> unavailableProductIds;

    public InsufficientStockException(Long orderId, List<String> unavailableProductIds) {
        super("Insufficient stock to reserve order " + orderId + " for products " + unavailableProductIds);
        this.unavailableProductIds = List.copyOf(unavailableProductIds);
    }
}
