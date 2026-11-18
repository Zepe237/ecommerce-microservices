package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.client.dto.AvailabilityResponse;
import com.feusap.ecommerce.order.client.dto.StockItemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
    name = "inventory-service",
    url = "${app.clients.inventory-service.url}",
    fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

    @PostMapping("/api/inventory/availability")
    AvailabilityResponse checkAvailability(@RequestBody List<StockItemRequest> items);
}
