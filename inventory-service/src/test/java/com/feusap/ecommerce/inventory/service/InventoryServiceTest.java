package com.feusap.ecommerce.inventory.service;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import com.feusap.ecommerce.inventory.domain.ProcessedOrder;
import com.feusap.ecommerce.inventory.dto.AvailabilityResponse;
import com.feusap.ecommerce.inventory.dto.StockItemRequest;
import com.feusap.ecommerce.inventory.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.inventory.repository.InventoryItemRepository;
import com.feusap.ecommerce.inventory.repository.ProcessedOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private ProcessedOrderRepository processedOrderRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("checkAvailability reports every product whose stock is missing or insufficient")
    void checkAvailability_reportsUnavailableProducts() {
        when(inventoryItemRepository.findAllById(any())).thenReturn(List.of(
                new InventoryItem("PRD-0001", 10),
                new InventoryItem("PRD-0002", 1)
        ));

        AvailabilityResponse response = inventoryService.checkAvailability(List.of(
                new StockItemRequest("PRD-0001", 3),
                new StockItemRequest("PRD-0002", 2),
                new StockItemRequest("PRD-0003", 1)
        ));

        assertThat(response.allAvailable()).isFalse();
        assertThat(response.unavailableProductIds()).containsExactly("PRD-0002", "PRD-0003");
    }

    @Test
    @DisplayName("checkAvailability sums the quantities requested for the same product")
    void checkAvailability_sumsDuplicateLines() {
        when(inventoryItemRepository.findAllById(any())).thenReturn(List.of(new InventoryItem("PRD-0001", 4)));

        AvailabilityResponse response = inventoryService.checkAvailability(List.of(
                new StockItemRequest("PRD-0001", 3),
                new StockItemRequest("PRD-0001", 2)
        ));

        assertThat(response.allAvailable()).isFalse();
    }

    @Test
    @DisplayName("deductStockForOrder ignores an order that was already processed")
    void deductStockForOrder_isIdempotent() {
        when(processedOrderRepository.existsById(42L)).thenReturn(true);

        inventoryService.deductStockForOrder(new OrderCreatedEvent(42L,
                List.of(new OrderCreatedEvent.OrderedItem("PRD-0001", 1))));

        verify(inventoryItemRepository, never()).deduct(anyString(), anyInt());
        verify(processedOrderRepository, never()).save(any(ProcessedOrder.class));
    }

    @Test
    @DisplayName("deductStockForOrder deducts each line and marks the order as processed")
    void deductStockForOrder_deductsEachLine() {
        when(processedOrderRepository.existsById(7L)).thenReturn(false);
        when(inventoryItemRepository.deduct(anyString(), anyInt())).thenReturn(1);

        inventoryService.deductStockForOrder(new OrderCreatedEvent(7L, List.of(
                new OrderCreatedEvent.OrderedItem("PRD-0001", 2),
                new OrderCreatedEvent.OrderedItem("PRD-0002", 1))));

        verify(inventoryItemRepository).deduct("PRD-0001", 2);
        verify(inventoryItemRepository).deduct("PRD-0002", 1);
        verify(processedOrderRepository).save(any(ProcessedOrder.class));
    }
}
