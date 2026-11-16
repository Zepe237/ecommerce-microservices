package com.feusap.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}
