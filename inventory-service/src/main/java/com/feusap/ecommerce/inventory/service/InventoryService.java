package com.feusap.ecommerce.inventory.service;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import com.feusap.ecommerce.inventory.domain.ProcessedOrder;
import com.feusap.ecommerce.inventory.domain.ReservationStatus;
import com.feusap.ecommerce.inventory.domain.StockReservation;
import com.feusap.ecommerce.inventory.dto.AvailabilityResponse;
import com.feusap.ecommerce.inventory.dto.StockItemRequest;
import com.feusap.ecommerce.inventory.dto.StockLevelResponse;
import com.feusap.ecommerce.inventory.exception.InsufficientStockException;
import com.feusap.ecommerce.inventory.exception.InventoryItemNotFoundException;
import com.feusap.ecommerce.inventory.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.inventory.repository.InventoryItemRepository;
import com.feusap.ecommerce.inventory.repository.ProcessedOrderRepository;
import com.feusap.ecommerce.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProcessedOrderRepository processedOrderRepository;
    private final StockReservationRepository stockReservationRepository;

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

    /**
     * Holds the stock of an order until its payment is known. All or nothing: when one product
     * is short, nothing is reserved. Idempotent: an order already reserved is ignored.
     */
    @Transactional
    public void reserve(Long orderId, List<StockItemRequest> items) {
        if (stockReservationRepository.existsByOrderId(orderId)) {
            log.info("Stock already reserved for order {}, skipping", orderId);
            return;
        }

        // Sorted by product id so that concurrent reservations always lock rows in the same order.
        Map<String, Integer> requested = items.stream()
                .collect(Collectors.toMap(StockItemRequest::productId, StockItemRequest::quantity,
                        Integer::sum, TreeMap::new));

        List<String> unavailable = new ArrayList<>();
        requested.forEach((productId, quantity) -> {
            if (inventoryItemRepository.reserve(productId, quantity) == 0) {
                unavailable.add(productId);
            }
        });
        if (!unavailable.isEmpty()) {
            // The exception rolls the whole transaction back, including the units already held above.
            throw new InsufficientStockException(orderId, unavailable);
        }

        requested.forEach((productId, quantity) ->
                stockReservationRepository.save(new StockReservation(orderId, productId, quantity)));
        log.info("Stock reserved for order {}: {}", orderId, requested);
    }

    /**
     * The order is confirmed: the held units leave the stock. Idempotent.
     */
    @Transactional
    public void commit(Long orderId) {
        List<StockReservation> reservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (StockReservation reservation : reservations) {
            inventoryItemRepository.commitReserved(reservation.getProductId(), reservation.getQuantity());
            reservation.commit();
        }
        log.info("Stock committed for order {} ({} line(s))", orderId, reservations.size());
    }

    /**
     * The order is cancelled: the held units are available again. Idempotent.
     */
    @Transactional
    public void release(Long orderId) {
        List<StockReservation> reservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (StockReservation reservation : reservations) {
            inventoryItemRepository.releaseReserved(reservation.getProductId(), reservation.getQuantity());
            reservation.release();
        }
        log.info("Stock released for order {} ({} line(s))", orderId, reservations.size());
    }

    private static boolean isAvailable(InventoryItem item, int requestedQuantity) {
        return item != null && item.getAvailable() >= requestedQuantity;
    }
}
