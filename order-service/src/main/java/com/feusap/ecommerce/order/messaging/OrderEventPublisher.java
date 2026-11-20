package com.feusap.ecommerce.order.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderConfirmedTopic;
    private final String orderCancelledTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.kafka.topics.order-confirmed}") String orderConfirmedTopic,
                               @Value("${app.kafka.topics.order-cancelled}") String orderCancelledTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderConfirmedTopic = orderConfirmedTopic;
        this.orderCancelledTopic = orderCancelledTopic;
    }

    /** Sends the event only once the current transaction has committed. */
    public void publishStatusChangedAfterCommit(OrderStatusChangedEvent event) {
        String topic = "CONFIRMED".equals(event.status()) ? orderConfirmedTopic : orderCancelledTopic;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order {}", topic, event.orderId(), ex);
                        } else {
                            log.info("Published {} for order {}", topic, event.orderId());
                        }
                    });
            }
        });
    }
}
