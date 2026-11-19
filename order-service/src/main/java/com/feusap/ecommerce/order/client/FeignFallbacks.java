package com.feusap.ecommerce.order.client;

import com.feusap.ecommerce.order.exception.DependencyUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class FeignFallbacks {

    private FeignFallbacks() {
    }

    static RuntimeException translate(String service, Throwable cause) {
        if (cause instanceof FeignException.FeignClientException clientError) {
            return clientError;          // 4xx : réponse métier, on la laisse passer
        }
        log.warn("{} unavailable: {}", service, cause.toString());
        return new DependencyUnavailableException(service, cause);
    }
}
