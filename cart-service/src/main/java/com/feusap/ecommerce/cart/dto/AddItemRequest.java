package com.feusap.ecommerce.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddItemRequest(
        @NotBlank String productId,
        @Positive int quantity
) {
}
