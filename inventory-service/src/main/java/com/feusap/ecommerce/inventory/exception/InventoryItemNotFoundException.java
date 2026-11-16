package com.feusap.ecommerce.inventory.exception;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(String productId) {
        super("No stock recorded for product " + productId);
    }
}
