package com.feusap.ecommerce.notification.messaging;

import com.feusap.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.order-created}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created for order {} of customer {}", event.orderId(), event.customerId());
        notificationService.sendOrderConfirmation(event);
    }
}
