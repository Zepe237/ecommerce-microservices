package com.feusap.ecommerce.payment_service.service;

import com.feusap.ecommerce.payment_service.client.OrderClient;
import com.feusap.ecommerce.payment_service.client.dto.OrderResponse;
import com.feusap.ecommerce.payment_service.domain.Payment;
import com.feusap.ecommerce.payment_service.domain.PaymentStatus;
import com.feusap.ecommerce.payment_service.dto.PaymentRequest;
import com.feusap.ecommerce.payment_service.dto.PaymentResponse;
import com.feusap.ecommerce.payment_service.exception.OrderNotPayableException;
import com.feusap.ecommerce.payment_service.messaging.PaymentCompletedEvent;
import com.feusap.ecommerce.payment_service.messaging.PaymentEventPublisher;
import com.feusap.ecommerce.payment_service.provider.PaymentProvider;
import com.feusap.ecommerce.payment_service.provider.PaymentResult;
import com.feusap.ecommerce.payment_service.provider.exception.PaymentProviderUnavailableException;
import com.feusap.ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long CUSTOMER_ID = 7L;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderClient orderClient;
    @Mock
    private PaymentProvider paymentProvider;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("An accepted charge is saved as SUCCEEDED and announced")
    void acceptedCharge() {
        givenNoExistingPayment();
        givenOrder("CONFIRMED", "99.99");
        givenSaveReturnsItsArgument();
        when(paymentProvider.charge(any(Payment.class), anyString())).thenReturn(PaymentResult.accepted("fake_123"));

        PaymentResponse response = paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_visa"));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.amount()).isEqualByComparingTo("99.99");
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);

        PaymentCompletedEvent event = capturePublishedEvent();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.status()).isEqualTo("SUCCEEDED");
        assertThat(event.failureReason()).isNull();
    }

    @Test
    @DisplayName("A declined charge is saved as FAILED and the reason travels in the event")
    void declinedCharge() {
        givenNoExistingPayment();
        givenOrder("CONFIRMED", "99.99");
        givenSaveReturnsItsArgument();
        when(paymentProvider.charge(any(Payment.class), anyString())).thenReturn(PaymentResult.declined("Card declined"));

        PaymentResponse response = paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_declined"));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureReason()).isEqualTo("Card declined");

        PaymentCompletedEvent event = capturePublishedEvent();
        assertThat(event.status()).isEqualTo("FAILED");
        assertThat(event.failureReason()).isEqualTo("Card declined");
    }

    @Test
    @DisplayName("An order already paid returns the existing payment without charging again")
    void alreadyPaidOrder() {
        Payment paid = Payment.builder()
                .id(10L)
                .orderId(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .amount(new BigDecimal("99.99"))
                .currency("cad")
                .status(PaymentStatus.SUCCEEDED)
                .build();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(paid));

        PaymentResponse response = paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_visa"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verifyNoInteractions(orderClient, paymentProvider, paymentEventPublisher);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("An order that is not payable is refused before any charge")
    void orderNotPayable() {
        givenNoExistingPayment();
        givenOrder("CANCELLED", "99.99");

        assertThatThrownBy(() -> paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_visa")))
                .isInstanceOf(OrderNotPayableException.class)
                .hasMessageContaining("CANCELLED");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentProvider, paymentEventPublisher);
    }

    @Test
    @DisplayName("A provider outage leaves the payment PENDING and announces nothing")
    void providerOutage() {
        givenNoExistingPayment();
        givenOrder("CONFIRMED", "99.99");
        givenSaveReturnsItsArgument();
        when(paymentProvider.charge(any(Payment.class), anyString()))
                .thenThrow(new PaymentProviderUnavailableException("down"));

        assertThatThrownBy(() -> paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_provider_down")))
                .isInstanceOf(PaymentProviderUnavailableException.class);

        verifyNoInteractions(paymentEventPublisher);
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isNotIn(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("A previous FAILED payment is reused, not duplicated, on retry")
    void retryAfterFailure() {
        Payment failed = Payment.builder()
                .id(10L)
                .orderId(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .amount(new BigDecimal("99.99"))
                .currency("cad")
                .status(PaymentStatus.FAILED)
                .failureReason("Card declined")
                .build();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(failed));
        givenOrder("CONFIRMED", "99.99");
        givenSaveReturnsItsArgument();
        when(paymentProvider.charge(any(Payment.class), anyString())).thenReturn(PaymentResult.accepted("fake_456"));

        PaymentResponse response = paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_visa"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(paymentRepository).save(failed);
    }

    @Test
    @DisplayName("The result is saved before the event is published")
    void savesBeforePublishing() {
        givenNoExistingPayment();
        givenOrder("CONFIRMED", "99.99");
        givenSaveReturnsItsArgument();
        when(paymentProvider.charge(any(Payment.class), anyString())).thenReturn(PaymentResult.accepted("fake_123"));

        paymentService.placePayment(new PaymentRequest(ORDER_ID, "pm_card_visa"));

        InOrder order = inOrder(paymentProvider, paymentRepository, paymentEventPublisher);
        order.verify(paymentProvider).charge(any(Payment.class), anyString());
        order.verify(paymentRepository).save(any(Payment.class));
        order.verify(paymentEventPublisher).publish(any(PaymentCompletedEvent.class));
    }

    private void givenNoExistingPayment() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
    }

    private void givenOrder(String status, String total) {
        when(orderClient.getOrder(ORDER_ID))
                .thenReturn(new OrderResponse(ORDER_ID, CUSTOMER_ID, status, new BigDecimal(total)));
    }

    private void givenSaveReturnsItsArgument() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PaymentCompletedEvent capturePublishedEvent() {
        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}
