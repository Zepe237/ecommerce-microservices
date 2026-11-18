package com.feusap.ecommerce.payment_service.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCompletedEvent(
    Long paymentId,
    Long orderId,
    Long customerId,
    BigDecimal amount,
    String currency,
    String status,          // "SUCCEEDED" ou "FAILED"
    String failureReason,   // null si SUCCEEDED
    Instant occurredAt
) {
}
