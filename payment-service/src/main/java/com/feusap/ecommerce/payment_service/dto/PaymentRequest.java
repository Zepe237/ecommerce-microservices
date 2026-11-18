package com.feusap.ecommerce.payment_service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record PaymentRequest(
    @NotNull Long orderId,
    @NotBlank String paymentMethod   // ex. "pm_card_visa" en mode test
) {}
