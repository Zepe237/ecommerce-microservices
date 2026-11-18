package com.feusap.ecommerce.notification.client;

import com.feusap.ecommerce.notification.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "customer-service",
    url = "${app.clients.customer-service.url}",
    fallbackFactory = CustomerClientFallbackFactory.class
)
public interface CustomerClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") Long customerId);
}
