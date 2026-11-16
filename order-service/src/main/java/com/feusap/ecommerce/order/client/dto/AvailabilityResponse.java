package com.feusap.ecommerce.order.client.dto;

import java.util.List;

public record AvailabilityResponse(boolean allAvailable, List<String> unavailableProductIds) {
}
