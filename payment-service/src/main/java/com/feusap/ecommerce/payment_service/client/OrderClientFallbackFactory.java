package com.feusap.ecommerce.payment_service.client;

import com.feusap.ecommerce.payment_service.client.dto.OrderResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    private static final String SERVICE = "order-service";
    @Override
    public  OrderClient create(Throwable cause) {
        return new OrderClient() {
            @Override
            public OrderResponse getOrder(Long customerId) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }

}
