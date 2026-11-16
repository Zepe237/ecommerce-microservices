package com.feusap.ecommerce.inventory.dto;

import java.util.List;

public record AvailabilityResponse(
        boolean allAvailable,
        List<String> unavailableProductIds
) {
}
