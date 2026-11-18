package com.feusap.ecommerce.payment_service.provider;
import com.feusap.ecommerce.payment_service.domain.Payment;

public interface PaymentProvider {
    PaymentResult charge(Payment payment, String paymentMethod);
}
