package com.feusap.ecommerce.inventory.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest(@PositiveOrZero int quantity) {
}
