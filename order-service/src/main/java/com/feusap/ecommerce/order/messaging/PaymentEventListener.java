package com.feusap.ecommerce.order.messaging;

import com.feusap.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "${app.kafka.topics.payment-completed}")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment.completed for order {} ({})", event.orderId(), event.status());
        orderService.handlePaymentResult(event.orderId(), "SUCCEEDED".equals(event.status()));
    }
}
