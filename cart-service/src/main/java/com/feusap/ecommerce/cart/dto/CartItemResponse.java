package com.feusap.ecommerce.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
