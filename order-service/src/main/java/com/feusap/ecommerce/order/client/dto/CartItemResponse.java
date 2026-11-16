package com.feusap.ecommerce.order.client.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}
