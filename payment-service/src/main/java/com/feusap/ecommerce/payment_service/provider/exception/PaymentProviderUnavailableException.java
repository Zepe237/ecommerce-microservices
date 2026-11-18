package com.feusap.ecommerce.payment_service.provider.exception;

public class PaymentProviderUnavailableException extends RuntimeException {

    public PaymentProviderUnavailableException(Throwable cause) {
        super("Payment provider is temporarily unavailable", cause);
    }

    public PaymentProviderUnavailableException(String message) {
        super(message);
    }
}
