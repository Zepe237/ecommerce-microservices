package com.feusap.ecommerce.notification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * History of e-mails sent. The unique index on orderId guarantees a single confirmation per order.
 */
@Document(collection = "notifications")
public record Notification(
        @Id String id,
        @Indexed(unique = true) Long orderId,
        @Indexed Long customerId,
        String recipient,
        String subject,
        Instant sentAt
) {

    public static Notification orderConfirmation(Long orderId, Long customerId, String recipient, String subject) {
        return new Notification(null, orderId, customerId, recipient, subject, Instant.now());
    }
}
