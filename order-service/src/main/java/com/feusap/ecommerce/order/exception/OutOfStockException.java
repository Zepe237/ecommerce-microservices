package com.feusap.ecommerce.order.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class OutOfStockException extends RuntimeException {

    private final List<String> productIds;

    public OutOfStockException(List<String> productIds) {
        super("Insufficient stock for products " + productIds);
        this.productIds = List.copyOf(productIds);
    }
}
