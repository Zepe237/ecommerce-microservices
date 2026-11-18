package com.feusap.ecommerce.payment_service.provider;


public record PaymentResult(boolean succeeded, String providerPaymentId, String failureReason) {

    public static PaymentResult accepted(String providerPaymentId) {
        return new PaymentResult(true, providerPaymentId, null);
    }

    public static PaymentResult declined(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
