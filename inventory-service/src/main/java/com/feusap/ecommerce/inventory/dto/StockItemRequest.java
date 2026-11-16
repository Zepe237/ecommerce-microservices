package com.feusap.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StockItemRequest(
        @NotBlank String productId,
        @Positive int quantity
) {
}
