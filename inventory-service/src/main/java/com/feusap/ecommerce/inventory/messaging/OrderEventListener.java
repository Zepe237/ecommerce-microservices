package com.feusap.ecommerce.inventory.messaging;

import com.feusap.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "${app.kafka.topics.order-confirmed}")
    public void onOrderConfirmed(OrderStatusChangedEvent event) {
        log.info("Received order.confirmed for order {}", event.orderId());
        inventoryService.commit(event.orderId());
    }

    @KafkaListener(topics = "${app.kafka.topics.order-cancelled}")
    public void onOrderCancelled(OrderStatusChangedEvent event) {
        log.info("Received order.cancelled for order {}", event.orderId());
        inventoryService.release(event.orderId());
    }
}
