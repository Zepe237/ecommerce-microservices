package com.feusap.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Units of a product held for an order until the order is confirmed or cancelled.
 * One reservation per product and per order.
 */
@Entity
@Table(name = "stock_reservations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StockReservation(Long orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
    }

    /** The order is confirmed: the reservation becomes a real deduction. */
    public void commit() {
        requireReserved();
        this.status = ReservationStatus.COMMITTED;
    }

    /** The order is cancelled: the units go back to the available stock. */
    public void release() {
        requireReserved();
        this.status = ReservationStatus.RELEASED;
    }

    private void requireReserved() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Reservation of " + productId + " for order " + orderId
                    + " is already " + status);
        }
    }
}
