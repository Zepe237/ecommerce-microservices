package com.feusap.ecommerce.payment_service.dto;

import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    Long id,
    Long orderId,
    Long customerId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String failureReason,
    Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getFailureReason(),
            payment.getCreatedAt()
        );
    }
}
