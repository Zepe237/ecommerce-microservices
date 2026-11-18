package com.feusap.ecommerce.payment_service.client;

import com.feusap.ecommerce.payment_service.client.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${app.clients.order-service.url}",
    fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {
    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable("orderId") Long orderId);
}
