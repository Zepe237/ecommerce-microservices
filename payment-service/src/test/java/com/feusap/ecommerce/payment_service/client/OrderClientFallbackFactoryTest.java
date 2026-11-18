package com.feusap.ecommerce.payment_service.client;

import com.feusap.ecommerce.payment_service.exception.DependencyUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderClientFallbackFactoryTest {

    private final OrderClientFallbackFactory factory = new OrderClientFallbackFactory();

    @Test
    @DisplayName("A 404 from order-service is a business answer and is rethrown unchanged")
    void rethrowsClientErrors() {
        FeignException notFound = FeignException.errorStatus("OrderClient#getOrder", response(404));

        assertThatThrownBy(() -> factory.create(notFound).getOrder(1L)).isSameAs(notFound);
    }

    @Test
    @DisplayName("A 5xx from order-service means it is unavailable")
    void translatesServerErrors() {
        FeignException serverError = FeignException.errorStatus("OrderClient#getOrder", response(500));

        assertThatThrownBy(() -> factory.create(serverError).getOrder(1L))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasFieldOrPropertyWithValue("service", "order-service")
                .hasCause(serverError);
    }

    @Test
    @DisplayName("A connection failure or an open circuit means order-service is unavailable")
    void translatesConnectionFailures() {
        RetryableException refused = new RetryableException(-1, "Connection refused",
                Request.HttpMethod.GET, (Long) null, request());

        assertThatThrownBy(() -> factory.create(refused).getOrder(1L))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://localhost/api/orders/1", Map.of(), null,
                StandardCharsets.UTF_8, null);
    }

    private static Response response(int status) {
        return Response.builder()
                .status(status)
                .reason("test")
                .request(request())
                .headers(Map.of())
                .build();
    }
}
