package com.feusap.ecommerce.order.service;

import com.feusap.ecommerce.order.client.CartClient;
import com.feusap.ecommerce.order.client.InventoryClient;
import com.feusap.ecommerce.order.client.dto.AvailabilityResponse;
import com.feusap.ecommerce.order.client.dto.CartItemResponse;
import com.feusap.ecommerce.order.client.dto.CartResponse;
import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.domain.OrderStatus;
import com.feusap.ecommerce.order.dto.OrderResponse;
import com.feusap.ecommerce.order.exception.EmptyCartException;
import com.feusap.ecommerce.order.exception.OutOfStockException;
import com.feusap.ecommerce.order.mapper.OrderMapper;
import com.feusap.ecommerce.order.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.order.messaging.OrderEventPublisher;
import com.feusap.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long CUSTOMER_ID = 1L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, new OrderMapper(), cartClient, inventoryClient,
                orderEventPublisher);
    }

    @Test
    @DisplayName("placeOrder confirms the order, computes the total and publishes order.created")
    void placeOrder_confirmsAndPublishes() {
        when(cartClient.getCart(CUSTOMER_ID)).thenReturn(new CartResponse(CUSTOMER_ID, List.of(
                new CartItemResponse("PRD-0001", "iPhone 15", new BigDecimal("1000.00"), 2),
                new CartItemResponse("PRD-0004", "AirPods Pro", new BigDecimal("300.00"), 1))));
        when(inventoryClient.checkAvailability(anyList())).thenReturn(new AvailabilityResponse(true, List.of()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(CUSTOMER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo("2300.00");
        assertThat(response.lines()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publishOrderCreated(event.capture());
        assertThat(event.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.getValue().items()).hasSize(2);
    }

    @Test
    @DisplayName("placeOrder rejects an empty cart without calling the inventory service")
    void placeOrder_rejectsEmptyCart() {
        when(cartClient.getCart(CUSTOMER_ID)).thenReturn(new CartResponse(CUSTOMER_ID, List.of()));

        assertThatThrownBy(() -> orderService.placeOrder(CUSTOMER_ID)).isInstanceOf(EmptyCartException.class);

        verifyNoInteractions(inventoryClient, orderRepository, orderEventPublisher);
    }

    @Test
    @DisplayName("placeOrder neither saves nor publishes when a product is out of stock")
    void placeOrder_rejectsOutOfStock() {
        when(cartClient.getCart(CUSTOMER_ID)).thenReturn(new CartResponse(CUSTOMER_ID, List.of(
                new CartItemResponse("PRD-0003", "MacBook Air M3", new BigDecimal("1499.00"), 20))));
        when(inventoryClient.checkAvailability(anyList()))
                .thenReturn(new AvailabilityResponse(false, List.of("PRD-0003")));

        assertThatThrownBy(() -> orderService.placeOrder(CUSTOMER_ID))
                .isInstanceOfSatisfying(OutOfStockException.class,
                        ex -> assertThat(ex.getProductIds()).containsExactly("PRD-0003"));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventPublisher);
    }
}
