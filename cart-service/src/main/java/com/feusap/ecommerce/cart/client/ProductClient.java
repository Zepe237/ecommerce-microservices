package com.feusap.ecommerce.cart.client;

import com.feusap.ecommerce.cart.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${app.clients.product-service.url}")
public interface ProductClient {

    @GetMapping("/api/products/{productId}")
    ProductResponse getProduct(@PathVariable("productId") String productId);
}
