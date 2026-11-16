package com.feusap.ecommerce.order.mapper;

import com.feusap.ecommerce.order.client.dto.CartItemResponse;
import com.feusap.ecommerce.order.client.dto.CartResponse;
import com.feusap.ecommerce.order.client.dto.StockItemRequest;
import com.feusap.ecommerce.order.domain.Order;
import com.feusap.ecommerce.order.domain.OrderLine;
import com.feusap.ecommerce.order.dto.OrderLineResponse;
import com.feusap.ecommerce.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public Order fromCart(CartResponse cart) {
        Order order = Order.forCustomer(cart.customerId());
        cart.items().forEach(item -> order.addLine(toLine(item)));
        return order;
    }

    public List<StockItemRequest> toStockItems(CartResponse cart) {
        return cart.items().stream()
                .map(item -> new StockItemRequest(item.productId(), item.quantity()))
                .toList();
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getLines().stream().map(this::toResponse).toList());
    }

    private OrderLine toLine(CartItemResponse item) {
        return new OrderLine(item.productId(), item.productName(), item.unitPrice(), item.quantity());
    }

    private OrderLineResponse toResponse(OrderLine line) {
        return new OrderLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getLineTotal());
    }
}
