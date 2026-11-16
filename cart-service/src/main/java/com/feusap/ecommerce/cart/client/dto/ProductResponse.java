package com.feusap.ecommerce.cart.client.dto;

import java.math.BigDecimal;

public record ProductResponse(String id, String name, BigDecimal price) {
}
