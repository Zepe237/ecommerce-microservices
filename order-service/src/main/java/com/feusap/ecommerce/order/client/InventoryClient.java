package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.client.dto.ReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "inventory-service",
    url = "${app.clients.inventory-service.url}",
    fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

    @PostMapping("/api/inventory/reservations")
    void reserve(@RequestBody ReservationRequest request);
}
