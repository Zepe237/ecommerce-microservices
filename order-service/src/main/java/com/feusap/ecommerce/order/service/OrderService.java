package com.feusap.ecommerce.order.service;

import com.feusap.ecommerce.order.client.CartClient;
import com.feusap.ecommerce.order.client.InventoryClient;
import com.feusap.ecommerce.order.client.dto.AvailabilityResponse;
import com.feusap.ecommerce.order.client.dto.CartResponse;
import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.dto.OrderResponse;
import com.feusap.ecommerce.order.exception.EmptyCartException;
import com.feusap.ecommerce.order.exception.OrderNotFoundException;
import com.feusap.ecommerce.order.exception.OutOfStockException;
import com.feusap.ecommerce.order.mapper.OrderMapper;
import com.feusap.ecommerce.order.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.order.messaging.OrderEventPublisher;
import com.feusap.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Places an order from the customer's current cart.
     * <ol>
     *     <li>loads the cart from the cart service,</li>
     *     <li>checks stock availability with the inventory service,</li>
     *     <li>persists the confirmed order,</li>
     *     <li>publishes order.created for the inventory, cart and notification services.</li>
     * </ol>
     * The method is intentionally not transactional: the event must only leave once the order is committed.
     */
    public OrderResponse placeOrder(Long customerId) {
        CartResponse cart = cartClient.getCart(customerId);
        if (cart.isEmpty()) {
            throw new EmptyCartException(customerId);
        }

        AvailabilityResponse availability = inventoryClient.checkAvailability(orderMapper.toStockItems(cart));
        if (!availability.allAvailable()) {
            throw new OutOfStockException(availability.unavailableProductIds());
        }

        Order order = orderMapper.fromCart(cart);
        order.confirm();
        Order saved = orderRepository.save(order);
        log.info("Order {} confirmed for customer {} ({} line(s), total {})",
                saved.getId(), customerId, saved.getLines().size(), saved.getTotalAmount());

        orderEventPublisher.publishOrderCreated(OrderCreatedEvent.from(saved));
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
}
