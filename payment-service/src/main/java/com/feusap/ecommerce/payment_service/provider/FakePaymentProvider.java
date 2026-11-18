package com.feusap.ecommerce.payment_service.provider;

import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.exception.PaymentProviderUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated payment provider for local development and failure testing.
 * Rules:
 * - amount above 1000            -> declined
 * - method "pm_card_declined"    -> declined
 * - method "pm_provider_down"    -> provider unavailable (exception)
 * - anything else                -> accepted
 */
@Slf4j
@Component
@Profile("!stripe")
public class FakePaymentProvider implements PaymentProvider {

    private static final BigDecimal LIMIT = new BigDecimal("1000");

    @Override
    public PaymentResult charge(Payment payment, String paymentMethod) {
        log.info("Fake charge of {} {} for order {}", payment.getAmount(), payment.getCurrency(), payment.getOrderId());

        if ("pm_provider_down".equals(paymentMethod)) {
            throw new PaymentProviderUnavailableException("Simulated provider outage");
        }
        if ("pm_card_declined".equals(paymentMethod)) {
            return PaymentResult.declined("Card declined");
        }
        if (payment.getAmount().compareTo(LIMIT) > 0) {
            return PaymentResult.declined("Amount exceeds the authorized limit");
        }
        return PaymentResult.accepted("fake_" + UUID.randomUUID());
    }
}
