package com.feusap.ecommerce.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records orders whose stock has already been deducted, so that a redelivered
 * Kafka message never deducts the same order twice.
 */
@Entity
@Table(name = "processed_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOrder {

    @Id
    private Long orderId;

    private Instant processedAt;

    public ProcessedOrder(Long orderId) {
        this.orderId = orderId;
        this.processedAt = Instant.now();
    }
}
