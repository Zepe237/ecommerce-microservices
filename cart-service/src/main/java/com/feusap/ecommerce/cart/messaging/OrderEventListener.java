package com.feusap.ecommerce.cart.messaging;

import com.feusap.ecommerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final CartService cartService;

    @KafkaListener(topics = "${app.kafka.topics.order-created}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order {} placed, emptying the cart of customer {}", event.orderId(), event.customerId());
        cartService.clear(event.customerId());
    }
}
