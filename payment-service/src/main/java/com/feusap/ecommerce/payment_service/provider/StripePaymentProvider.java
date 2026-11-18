package com.feusap.ecommerce.payment_service.provider;

import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.provider.exception.PaymentProviderUnavailableException;
import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final StripeClient stripe;

    public StripePaymentProvider(@Value("${app.stripe.secret-key}") String secretKey) {
        this.stripe = new StripeClient(secretKey);
    }

    @Override
    public PaymentResult charge(Payment payment, String paymentMethod) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(payment.getAmount().movePointRight(2).longValueExact()) // en centimes
            .setCurrency(payment.getCurrency())
            .setPaymentMethod(paymentMethod)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                    .build())
            .setConfirm(true)
            .putMetadata("orderId", String.valueOf(payment.getOrderId()))
            .build();

        RequestOptions options = RequestOptions.builder()
            .setIdempotencyKey("order-" + payment.getOrderId())
            .build();

        try {
            PaymentIntent intent = stripe.v1().paymentIntents().create(params, options);
            if ("succeeded".equals(intent.getStatus())) {
                return PaymentResult.accepted(intent.getId());
            }
            return PaymentResult.declined("Payment not completed: " + intent.getStatus());
        } catch (CardException e) {
            // La carte est refusée : c'est une réponse métier
            return PaymentResult.declined(e.getMessage());
        } catch (StripeException e) {
            // Réseau, clé invalide, Stripe indisponible : c'est une panne, pas un refus
            log.error("Stripe unavailable for order {}: {}", payment.getOrderId(), e.getMessage());
            throw new PaymentProviderUnavailableException(e);
        }
    }
}
