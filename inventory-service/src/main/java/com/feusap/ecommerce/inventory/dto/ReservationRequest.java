package com.feusap.ecommerce.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservationRequest(
        @NotNull Long orderId,
        @NotEmpty List<@Valid StockItemRequest> items
) {
}
