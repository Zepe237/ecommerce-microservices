package com.feusap.ecommerce.payment_service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderNotPayableException.class)
    public ProblemDetail handleOrderNotPayable(OrderNotPayableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyInProgressException.class)
    public ProblemDetail handlePaymentAlreadyInProgress(PaymentAlreadyInProgressException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ProblemDetail handleOrderNotFound(FeignException.NotFound ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order not found");
    }

    @ExceptionHandler(PaymentProviderUnavailableException.class)
    public ProblemDetail handleProviderUnavailable(PaymentProviderUnavailableException ex) {
        log.error("Payment provider unavailable", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            "Payment provider is temporarily unavailable, please retry later");
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ProblemDetail handleDependencyUnavailable(DependencyUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            ex.getMessage() + ", please retry later");
        problem.setProperty("service", ex.getService());
        return problem;
    }
}
