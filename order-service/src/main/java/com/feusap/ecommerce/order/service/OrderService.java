package com.feusap.ecommerce.order.service;

import com.feusap.ecommerce.order.client.CartClient;
import com.feusap.ecommerce.order.client.InventoryClient;
import com.feusap.ecommerce.order.client.dto.CartResponse;
import com.feusap.ecommerce.order.client.dto.ReservationRequest;
import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.domain.OrderStatus;
import com.feusap.ecommerce.order.dto.OrderResponse;
import com.feusap.ecommerce.order.exception.EmptyCartException;
import com.feusap.ecommerce.order.exception.OrderNotFoundException;
import com.feusap.ecommerce.order.exception.OutOfStockException;
import com.feusap.ecommerce.order.mapper.OrderMapper;
import com.feusap.ecommerce.order.messaging.OrderEventPublisher;
import com.feusap.ecommerce.order.messaging.OrderStatusChangedEvent;
import com.feusap.ecommerce.order.messaging.PaymentCompletedEvent;
import com.feusap.ecommerce.order.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Places an order from the customer's current cart and starts the payment saga.
     * <ol>
     *     <li>loads the cart from the cart service,</li>
     *     <li>persists the order as PAYMENT_PENDING (this gives it an id),</li>
     *     <li>reserves its stock with the inventory service, all or nothing,</li>
     *     <li>on insufficient stock, cancels the order (compensation) and throws OutOfStockException.</li>
     * </ol>
     * The order is confirmed or cancelled later, when payment.completed arrives (see handlePaymentResult).
     * The method is intentionally not transactional: the order must be committed before calling the inventory.
     */
    public OrderResponse placeOrder(Long customerId) {
        CartResponse cart = cartClient.getCart(customerId);
        if (cart.isEmpty()) {
            throw new EmptyCartException(customerId);
        }
        Order order = orderMapper.fromCart(cart);
        order.markPaymentPending();
        Order saved = orderRepository.save(order);
        try {
            inventoryClient.reserve(new ReservationRequest(saved.getId(), orderMapper.toStockItems(cart)));
        } catch (FeignException.Conflict e) {
            saved.cancel();                          // compensation
            orderRepository.save(saved);
            log.warn("Order {} cancelled: insufficient stock", saved.getId());
            throw new OutOfStockException(List.of());
        }

        log.info("Order {} awaiting payment for customer {}", saved.getId(), customerId);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long orderId) {
        return orderRepository.findWithLinesById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }
    @Transactional
    public void handlePaymentResult(Long orderId, boolean paid) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Idempotence : Kafka peut livrer deux fois le même message
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.info("Order {} already {}, payment result ignored", orderId, order.getStatus());
            return;
        }

        if (paid) {
            order.confirm();
        } else {
            order.cancel();
        }
        orderEventPublisher.publishStatusChangedAfterCommit(OrderStatusChangedEvent.from(order));
        log.info("Order {} is now {}", orderId, order.getStatus());
    }
    /**
     * Cancels the orders still awaiting payment before the cutoff, so that their reserved stock is released.
     */
    @Transactional
    public int expireUnpaidOrders(Instant cutoff) {
        List<Order> expired = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoff);
        for (Order order : expired) {
            order.cancel();
            orderEventPublisher.publishStatusChangedAfterCommit(OrderStatusChangedEvent.from(order));
            log.info("Order {} expired: no payment received in time", order.getId());
        }
        return expired.size();
    }
}
