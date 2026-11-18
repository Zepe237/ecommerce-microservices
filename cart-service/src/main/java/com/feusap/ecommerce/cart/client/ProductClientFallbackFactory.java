package com.feusap.ecommerce.cart.client;

import com.feusap.ecommerce.cart.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final String SERVICE = "product-service";

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductResponse getProduct(String productId) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }
}
