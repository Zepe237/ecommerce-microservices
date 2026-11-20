package com.feusap.ecommerce.inventory.domain;

/**
 * Lifecycle of a stock reservation within the payment saga.
 */
public enum ReservationStatus {
    /** Units are held for an order waiting for its payment. */
    RESERVED,
    /** The order is confirmed: the held units have left the stock. */
    COMMITTED,
    /** The order is cancelled: the held units are available again. */
    RELEASED
}
