package com.feusap.ecommerce.cart.exception;

import lombok.Getter;

@Getter
public class DependencyUnavailableException extends RuntimeException {

    private final String service;

    public DependencyUnavailableException(String service, Throwable cause) {
        super(service + " is temporarily unavailable", cause);
        this.service = service;
    }
}
