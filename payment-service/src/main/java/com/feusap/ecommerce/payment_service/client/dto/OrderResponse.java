package com.feusap.ecommerce.payment_service.client.dto;

import java.math.BigDecimal;

public record OrderResponse(
    Long id,
    Long customerId,
    String status,
    BigDecimal totalAmount
) {
}
