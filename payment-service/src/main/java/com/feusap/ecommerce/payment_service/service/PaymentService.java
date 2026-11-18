package com.feusap.ecommerce.payment_service.service;

import com.feusap.ecommerce.payment_service.client.OrderClient;
import com.feusap.ecommerce.payment_service.client.dto.OrderResponse;
import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.domain.PaymentStatus;
import com.feusap.ecommerce.payment_service.dto.PaymentRequest;
import com.feusap.ecommerce.payment_service.dto.PaymentResponse;
import com.feusap.ecommerce.payment_service.exception.OrderNotPayableException;
import com.feusap.ecommerce.payment_service.exception.PaymentAlreadyInProgressException;
import com.feusap.ecommerce.payment_service.messaging.PaymentCompletedEvent;
import com.feusap.ecommerce.payment_service.messaging.PaymentEventPublisher;
import com.feusap.ecommerce.payment_service.provider.PaymentProvider;
import com.feusap.ecommerce.payment_service.provider.PaymentResult;
import com.feusap.ecommerce.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentProvider paymentProvider;

    public PaymentResponse placePayment(PaymentRequest request) {
        // 1. Idempotence : déjà payée ?
        Optional<Payment> existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.SUCCEEDED) {
            return PaymentResponse.from(existing.get());
        }

        // 2. La commande fait foi pour le montant et le client
        OrderResponse order = orderClient.getOrder(request.orderId());
        if(!"CONFIRMED".equals(order.status())){
            throw new OrderNotPayableException(order.id(), order.status());
        }

        // 3. Trace PENDING enregistrée AVANT l'appel externe
        Payment payment = existing.orElseGet(() -> {
            try {
                return paymentRepository.saveAndFlush(Payment.builder()
                    .orderId(order.id())
                    .customerId(order.customerId())
                    .amount(order.totalAmount())
                    .currency("cad")
                    .build());
            } catch (DataIntegrityViolationException e) {
                throw new PaymentAlreadyInProgressException(order.id());
            }
        });

        // 4. Appel au prestataire (Stripe ou faux prestataire)
        PaymentResult result = paymentProvider.charge(payment, request.paymentMethod());

        // 5. Résultat enregistré, PUIS annoncé
        if (result.succeeded()) {
            payment.markSucceeded(result.providerPaymentId());
        } else {
            payment.markFailed(result.failureReason());
        }
        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publish(toEvent(saved));

        return PaymentResponse.from(saved);
    }

    private PaymentCompletedEvent toEvent(Payment payment) {
        return new PaymentCompletedEvent(
            payment.getId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus().name(),
            payment.getFailureReason(),
            Instant.now()
        );
    }
}
