package com.feusap.ecommerce.order.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
