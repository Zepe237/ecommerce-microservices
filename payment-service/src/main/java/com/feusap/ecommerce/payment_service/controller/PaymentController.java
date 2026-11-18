package com.feusap.ecommerce.payment_service.controller;

import com.feusap.ecommerce.payment_service.dto.PaymentRequest;
import com.feusap.ecommerce.payment_service.dto.PaymentResponse;
import com.feusap.ecommerce.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment controller")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @Operation(
        summary = "Pay an order",
        description = "create a payment"
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@RequestBody @Valid PaymentRequest paymentRequest){
        return paymentService.placePayment(paymentRequest);
    }
}
