package com.feusap.ecommerce.payment_service.dto;

import com.feusap.ecommerce.payment_service.domain.PaymentStatus;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequest(
    @NotNull Long orderId,
    @NotBlank String paymentMethod   // ex. "pm_card_visa" en mode test
) {}
