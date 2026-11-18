package com.feusap.ecommerce.payment_service.exception;

public class OrderNotPayableException extends RuntimeException {

    public OrderNotPayableException(Long orderId, String orderStatus) {
        super("Order " + orderId + " is not in a payable status (current status: " + orderStatus + ")");
    }
}
