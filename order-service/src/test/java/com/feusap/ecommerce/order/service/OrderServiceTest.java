package com.feusap.ecommerce.order.service;

import com.feusap.ecommerce.order.client.CartClient;
import com.feusap.ecommerce.order.client.InventoryClient;
import com.feusap.ecommerce.order.client.dto.CartItemResponse;
import com.feusap.ecommerce.order.client.dto.CartResponse;
import com.feusap.ecommerce.order.client.dto.ReservationRequest;
import com.feusap.ecommerce.order.client.dto.StockItemRequest;
import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.domain.OrderStatus;
import com.feusap.ecommerce.order.dto.OrderResponse;
import com.feusap.ecommerce.order.exception.DependencyUnavailableException;
import com.feusap.ecommerce.order.exception.EmptyCartException;
import com.feusap.ecommerce.order.exception.OutOfStockException;
import com.feusap.ecommerce.order.mapper.OrderMapper;
import com.feusap.ecommerce.order.messaging.OrderEventPublisher;
import com.feusap.ecommerce.order.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ORDER_ID = 42L;

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
    @DisplayName("placeOrder saves the order as PAYMENT_PENDING and reserves its stock")
    void placeOrder_reservesStockAndAwaitsPayment() {
        givenCartWithTwoProducts();
        givenRepositoryAssignsAnId();

        OrderResponse response = orderService.placeOrder(CUSTOMER_ID);

        assertThat(response.id()).isEqualTo(ORDER_ID);
        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("2300.00");
        assertThat(response.lines()).hasSize(2);

        ArgumentCaptor<ReservationRequest> request = ArgumentCaptor.forClass(ReservationRequest.class);
        verify(inventoryClient).reserve(request.capture());
        assertThat(request.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(request.getValue().items()).containsExactly(
                new StockItemRequest("PRD-0001", 2),
                new StockItemRequest("PRD-0004", 1));

        // Nothing is published yet: the order is only confirmed once the payment succeeds.
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    @DisplayName("placeOrder cancels the order when the inventory answers 409 (insufficient stock)")
    void placeOrder_cancelsOrderWhenStockIsInsufficient() {
        givenCartWithTwoProducts();
        givenRepositoryAssignsAnId();
        doThrow(mock(FeignException.Conflict.class)).when(inventoryClient).reserve(any());

        assertThatThrownBy(() -> orderService.placeOrder(CUSTOMER_ID))
                .isInstanceOf(OutOfStockException.class);

        // Saved twice: first PAYMENT_PENDING, then CANCELLED by the compensation.
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    @DisplayName("placeOrder leaves the order PAYMENT_PENDING when the inventory is unavailable")
    void placeOrder_keepsOrderPendingWhenInventoryIsDown() {
        givenCartWithTwoProducts();
        givenRepositoryAssignsAnId();
        doThrow(new DependencyUnavailableException("inventory-service", new RuntimeException("timeout")))
                .when(inventoryClient).reserve(any());

        assertThatThrownBy(() -> orderService.placeOrder(CUSTOMER_ID))
                .isInstanceOf(DependencyUnavailableException.class);

        // No compensation here: the reservation may have succeeded. The timeout (step 7) will clean up.
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("placeOrder rejects an empty cart without calling the inventory service")
    void placeOrder_rejectsEmptyCart() {
        when(cartClient.getCart(CUSTOMER_ID)).thenReturn(new CartResponse(CUSTOMER_ID, List.of()));

        assertThatThrownBy(() -> orderService.placeOrder(CUSTOMER_ID)).isInstanceOf(EmptyCartException.class);

        verifyNoInteractions(inventoryClient, orderRepository, orderEventPublisher);
    }

    private void givenCartWithTwoProducts() {
        when(cartClient.getCart(CUSTOMER_ID)).thenReturn(new CartResponse(CUSTOMER_ID, List.of(
                new CartItemResponse("PRD-0001", "iPhone 15", new BigDecimal("1000.00"), 2),
                new CartItemResponse("PRD-0004", "AirPods Pro", new BigDecimal("300.00"), 1))));
    }

    /** Simulates the database: the first save gives the order its id, like PostgreSQL would. */
    private void givenRepositoryAssignsAnId() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                ReflectionTestUtils.setField(order, "id", ORDER_ID);
            }
            return order;
        });
    }
}
