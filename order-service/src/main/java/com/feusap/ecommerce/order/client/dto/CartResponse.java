package com.feusap.ecommerce.order.client.dto;

import java.util.List;

public record CartResponse(Long customerId, List<CartItemResponse> items) {

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
