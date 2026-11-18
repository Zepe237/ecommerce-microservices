package com.feusap.ecommerce.payment_service.messaging;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventPublisher {
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final String paymentCompletedTopic;
    public PaymentEventPublisher(KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate,
                                 @Value("${app.kafka.topics.payment-completed}") String paymentCompletedTopic){
        this.kafkaTemplate = kafkaTemplate;
        this.paymentCompletedTopic = paymentCompletedTopic;

    }
    public void publish(PaymentCompletedEvent event) {
        String key = String.valueOf(event.orderId());
        kafkaTemplate.send(paymentCompletedTopic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish payment {} for order {}", event.paymentId(), event.orderId(), ex);
                } else {
                    log.info("Published payment {} for order {} with status {} (partition {}, offset {})",
                        event.paymentId(), event.orderId(), event.status(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
    }
}
