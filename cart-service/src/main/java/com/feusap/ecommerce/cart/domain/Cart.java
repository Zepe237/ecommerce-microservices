package com.feusap.ecommerce.cart.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer has at most one active cart, keyed by the customer identifier.
 */
@Getter
@RedisHash("carts")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Cart {

    @Id
    private Long customerId;

    private List<CartItem> items = new ArrayList<>();

    public Cart(Long customerId) {
        this.customerId = customerId;
    }

    public void addItem(String productId, String productName, BigDecimal unitPrice, int quantity) {
        items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> items.add(new CartItem(productId, productName, unitPrice, quantity)));
    }

    public void removeItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
