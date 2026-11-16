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

    @KafkaListener(topics = "${app.kafka.topics.order-created}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created for order {}", event.orderId());
        inventoryService.deductStockForOrder(event);
    }
}
