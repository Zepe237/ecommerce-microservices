package com.feusap.ecommerce.order.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException(Long customerId) {
        super("The cart of customer " + customerId + " is empty");
    }
}
