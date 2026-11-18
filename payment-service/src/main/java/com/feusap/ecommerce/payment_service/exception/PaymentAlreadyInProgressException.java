package com.feusap.ecommerce.payment_service.exception;

public class PaymentAlreadyInProgressException extends RuntimeException {

    public PaymentAlreadyInProgressException(Long orderId) {
        super("A payment is already in progress for order " + orderId + ", please retry in a moment");
    }
}
