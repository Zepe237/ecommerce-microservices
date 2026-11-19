package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.client.dto.CartResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    private static final String SERVICE = "cart-service";

    @Override
    public CartClient create(Throwable cause) {
        return new CartClient() {
            @Override
            public CartResponse getCart(Long customerId) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }
}
