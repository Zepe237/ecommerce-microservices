package com.feusap.ecommerce.notification.client;

import com.feusap.ecommerce.notification.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    private static final String SERVICE = "customer-service";

    @Override
    public CustomerClient create(Throwable cause) {
        return new CustomerClient() {
            @Override
            public CustomerResponse getCustomer(Long customerId) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }
}
