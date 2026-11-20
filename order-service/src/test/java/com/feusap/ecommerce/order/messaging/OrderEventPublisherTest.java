package com.feusap.ecommerce.order.messaging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * There is no real transaction under Mockito, so the test opens Spring's synchronization registry by hand
 * and plays the role of the transaction manager: it decides when the "commit" happens.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OrderEventPublisher(kafkaTemplate, "order.created", "order.confirmed", "order.cancelled");
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("Nothing leaves before the commit; a confirmed order goes to order.confirmed after it")
    void confirmedOrderIsSentAfterCommit() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        OrderStatusChangedEvent event = event("CONFIRMED");

        publisher.publishStatusChangedAfterCommit(event);
        verifyNoInteractions(kafkaTemplate);                 // still inside the transaction

        commit();
        verify(kafkaTemplate).send(eq("order.confirmed"), eq("42"), eq(event));
    }

    @Test
    @DisplayName("A cancelled order goes to order.cancelled")
    void cancelledOrderGoesToCancelledTopic() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        OrderStatusChangedEvent event = event("CANCELLED");

        publisher.publishStatusChangedAfterCommit(event);
        commit();

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("42"), eq(event));
    }

    @Test
    @DisplayName("A rolled-back transaction publishes nothing")
    void rollbackPublishesNothing() {
        publisher.publishStatusChangedAfterCommit(event("CONFIRMED"));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(kafkaTemplate);
    }

    private static OrderStatusChangedEvent event(String status) {
        return new OrderStatusChangedEvent(42L, 1L, status, new BigDecimal("99.99"), Instant.now());
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
