package com.feusap.ecommerce.inventory.dto;

import com.feusap.ecommerce.inventory.domain.InventoryItem;

public record StockLevelResponse(
        String productId,
        int quantity,
        int reserved,
        int available
) {

    public static StockLevelResponse from(InventoryItem item) {
        return new StockLevelResponse(item.getProductId(), item.getQuantity(), item.getReserved(), item.getAvailable());
    }
}
