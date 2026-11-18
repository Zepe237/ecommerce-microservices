package com.feusap.ecommerce.payment_service.client;

import com.feusap.ecommerce.payment_service.exception.DependencyUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignFallbacks {
    private FeignFallbacks(){

    }
    static RuntimeException translate(String service, Throwable cause){
        if(cause instanceof FeignException.FeignClientException clienError){
            return clienError ;
        }
        log.warn("{} unavailable: {}", service, cause.toString());
        return new DependencyUnavailableException(service, cause);
    }
}
