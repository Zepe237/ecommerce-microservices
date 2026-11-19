package com.feusap.ecommerce.inventory.service;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import com.feusap.ecommerce.inventory.domain.ProcessedOrder;
import com.feusap.ecommerce.inventory.domain.ReservationStatus;
import com.feusap.ecommerce.inventory.domain.StockReservation;
import com.feusap.ecommerce.inventory.dto.AvailabilityResponse;
import com.feusap.ecommerce.inventory.dto.StockItemRequest;
import com.feusap.ecommerce.inventory.exception.InsufficientStockException;
import com.feusap.ecommerce.inventory.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.inventory.repository.InventoryItemRepository;
import com.feusap.ecommerce.inventory.repository.ProcessedOrderRepository;
import com.feusap.ecommerce.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private ProcessedOrderRepository processedOrderRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

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

    @Test
    @DisplayName("reserve holds every product and records one reservation per product")
    void reserve_holdsEveryProduct() {
        when(stockReservationRepository.existsByOrderId(1L)).thenReturn(false);
        when(inventoryItemRepository.reserve(anyString(), anyInt())).thenReturn(1);

        inventoryService.reserve(1L, List.of(
                new StockItemRequest("PRD-0001", 2),
                new StockItemRequest("PRD-0002", 1)));

        verify(inventoryItemRepository).reserve("PRD-0001", 2);
        verify(inventoryItemRepository).reserve("PRD-0002", 1);
        ArgumentCaptor<StockReservation> saved = ArgumentCaptor.forClass(StockReservation.class);
        verify(stockReservationRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(StockReservation::getProductId, StockReservation::getQuantity, StockReservation::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("PRD-0001", 2, ReservationStatus.RESERVED),
                        org.assertj.core.groups.Tuple.tuple("PRD-0002", 1, ReservationStatus.RESERVED));
    }

    @Test
    @DisplayName("reserve sums the quantities requested for the same product")
    void reserve_sumsDuplicateLines() {
        when(stockReservationRepository.existsByOrderId(1L)).thenReturn(false);
        when(inventoryItemRepository.reserve(anyString(), anyInt())).thenReturn(1);

        inventoryService.reserve(1L, List.of(
                new StockItemRequest("PRD-0001", 2),
                new StockItemRequest("PRD-0001", 3)));

        verify(inventoryItemRepository).reserve("PRD-0001", 5);
        verify(stockReservationRepository, times(1)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("reserve refuses the whole order when one product is short, and records nothing")
    void reserve_refusesWhenStockIsShort() {
        when(stockReservationRepository.existsByOrderId(1L)).thenReturn(false);
        when(inventoryItemRepository.reserve("PRD-0001", 2)).thenReturn(1);
        when(inventoryItemRepository.reserve("PRD-0002", 9)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.reserve(1L, List.of(
                new StockItemRequest("PRD-0001", 2),
                new StockItemRequest("PRD-0002", 9))))
                .isInstanceOf(InsufficientStockException.class)
                .extracting("unavailableProductIds")
                .isEqualTo(List.of("PRD-0002"));

        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("reserve ignores an order that is already reserved")
    void reserve_isIdempotent() {
        when(stockReservationRepository.existsByOrderId(1L)).thenReturn(true);

        inventoryService.reserve(1L, List.of(new StockItemRequest("PRD-0001", 2)));

        verifyNoInteractions(inventoryItemRepository);
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("commit turns every held line into a deduction")
    void commit_deductsHeldUnits() {
        StockReservation reservation = new StockReservation(1L, "PRD-0001", 2);
        when(stockReservationRepository.findByOrderIdAndStatus(1L, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));

        inventoryService.commit(1L);

        verify(inventoryItemRepository).commitReserved("PRD-0001", 2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    @DisplayName("release gives every held line back to the available stock")
    void release_givesHeldUnitsBack() {
        StockReservation reservation = new StockReservation(1L, "PRD-0001", 2);
        when(stockReservationRepository.findByOrderIdAndStatus(1L, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));

        inventoryService.release(1L);

        verify(inventoryItemRepository).releaseReserved("PRD-0001", 2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("commit or release with nothing held does nothing (redelivered message)")
    void commitAndRelease_doNothingWithoutHeldUnits() {
        when(stockReservationRepository.findByOrderIdAndStatus(1L, ReservationStatus.RESERVED))
                .thenReturn(List.of());

        inventoryService.commit(1L);
        inventoryService.release(1L);

        verifyNoInteractions(inventoryItemRepository);
    }
}
