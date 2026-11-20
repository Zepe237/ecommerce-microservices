package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.client.dto.ReservationRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Invoked by the circuit breaker when a call to inventory-service fails or when the circuit is open.
 * No degraded answer is made up: the failure is turned into a clear, fast error.
 */
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final String SERVICE = "inventory-service";

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public void reserve(ReservationRequest request) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }
}
