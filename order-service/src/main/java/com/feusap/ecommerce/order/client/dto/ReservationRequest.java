package com.feusap.ecommerce.order.client.dto;

import java.util.List;

public record ReservationRequest(
    Long orderId,
    List<StockItemRequest> items
) {
}
