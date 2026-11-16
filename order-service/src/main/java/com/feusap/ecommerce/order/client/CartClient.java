package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.client.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${app.clients.cart-service.url}")
public interface CartClient {

    @GetMapping("/api/carts/{customerId}")
    CartResponse getCart(@PathVariable("customerId") Long customerId);
}
