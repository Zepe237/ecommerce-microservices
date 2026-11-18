package com.feusap.ecommerce.payment_service.provider;

import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.exception.PaymentProviderUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakePaymentProviderTest {

    private final FakePaymentProvider provider = new FakePaymentProvider();

    @Test
    @DisplayName("A regular card under the limit is accepted")
    void acceptsRegularCard() {
        PaymentResult result = provider.charge(payment("99.99"), "pm_card_visa");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.providerPaymentId()).startsWith("fake_");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("The declined test card is refused")
    void refusesDeclinedCard() {
        PaymentResult result = provider.charge(payment("99.99"), "pm_card_declined");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).isEqualTo("Card declined");
    }

    @Test
    @DisplayName("An amount above 1000 is refused")
    void refusesAmountAboveLimit() {
        PaymentResult result = provider.charge(payment("2199.98"), "pm_card_visa");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).contains("limit");
    }

    @Test
    @DisplayName("An amount of exactly 1000 is still accepted")
    void acceptsAmountAtLimit() {
        assertThat(provider.charge(payment("1000.00"), "pm_card_visa").succeeded()).isTrue();
    }

    @Test
    @DisplayName("The outage test method simulates an unavailable provider")
    void simulatesOutage() {
        assertThatThrownBy(() -> provider.charge(payment("99.99"), "pm_provider_down"))
                .isInstanceOf(PaymentProviderUnavailableException.class);
    }

    private static Payment payment(String amount) {
        return Payment.builder()
                .orderId(1L)
                .customerId(7L)
                .amount(new BigDecimal(amount))
                .currency("cad")
                .build();
    }
}
