package com.feusap.ecommerce.inventory.service;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import com.feusap.ecommerce.inventory.domain.ProcessedOrder;
import com.feusap.ecommerce.inventory.dto.AvailabilityResponse;
import com.feusap.ecommerce.inventory.dto.StockItemRequest;
import com.feusap.ecommerce.inventory.dto.StockLevelResponse;
import com.feusap.ecommerce.inventory.exception.InventoryItemNotFoundException;
import com.feusap.ecommerce.inventory.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.inventory.repository.InventoryItemRepository;
import com.feusap.ecommerce.inventory.repository.ProcessedOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProcessedOrderRepository processedOrderRepository;

    /**
     * Checks that every requested product has enough available stock. Quantities requested
     * several times for the same product are summed before the check.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(List<StockItemRequest> items) {
        Map<String, Integer> requested = items.stream()
                .collect(Collectors.toMap(StockItemRequest::productId, StockItemRequest::quantity, Integer::sum));

        Map<String, InventoryItem> stock = inventoryItemRepository.findAllById(requested.keySet()).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        List<String> unavailable = requested.entrySet().stream()
                .filter(entry -> !isAvailable(stock.get(entry.getKey()), entry.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!unavailable.isEmpty()) {
            log.info("Insufficient stock for products {}", unavailable);
        }
        return new AvailabilityResponse(unavailable.isEmpty(), unavailable);
    }

    @Transactional(readOnly = true)
    public StockLevelResponse getStockLevel(String productId) {
        return inventoryItemRepository.findById(productId)
                .map(StockLevelResponse::from)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));
    }

    /**
     * Sets the physical stock of a product, creating the inventory line when it does not exist yet.
     */
    @Transactional
    public StockLevelResponse setStock(String productId, int quantity) {
        InventoryItem item = inventoryItemRepository.findById(productId)
                .orElseGet(() -> new InventoryItem(productId, 0));
        item.updateQuantity(quantity);
        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Stock of product {} set to {}", productId, quantity);
        return StockLevelResponse.from(saved);
    }

    /**
     * Deducts the stock of a confirmed order. Idempotent: an order already processed is ignored.
     */
    @Transactional
    public void deductStockForOrder(OrderCreatedEvent event) {
        if (processedOrderRepository.existsById(event.orderId())) {
            log.info("Order {} already processed, skipping", event.orderId());
            return;
        }
        for (OrderCreatedEvent.OrderedItem item : event.items()) {
            int updated = inventoryItemRepository.deduct(item.productId(), item.quantity());
            if (updated == 0) {
                log.warn("Could not deduct {} unit(s) of product {} for order {}: insufficient stock",
                        item.quantity(), item.productId(), event.orderId());
            }
        }
        processedOrderRepository.save(new ProcessedOrder(event.orderId()));
        log.info("Stock deducted for order {}", event.orderId());
    }

    private static boolean isAvailable(InventoryItem item, int requestedQuantity) {
        return item != null && item.getAvailable() >= requestedQuantity;
    }
}
