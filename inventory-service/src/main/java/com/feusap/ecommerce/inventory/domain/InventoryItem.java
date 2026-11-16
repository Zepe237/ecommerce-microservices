package com.feusap.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stock level of a single product. The identifier is the product identifier
 * issued by the product service; it is assigned, never generated.
 */
@Entity
@Table(name = "inventory_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {

    @Id
    @Column(name = "product_id", length = 64)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    /** Units held for orders in progress. Not used yet, reserved for the reservation workflow. */
    @Column(nullable = false)
    private int reserved;

    @Version
    private Long version;

    public InventoryItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getAvailable() {
        return quantity - reserved;
    }

    public void updateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.quantity = quantity;
    }
}
