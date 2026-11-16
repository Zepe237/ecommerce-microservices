package com.feusap.ecommerce.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long orderId,
        Long customerId,
        String recipient,
        String subject,
        Instant sentAt
) {
}
