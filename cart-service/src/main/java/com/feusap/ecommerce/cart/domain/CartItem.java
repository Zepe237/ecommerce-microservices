package com.feusap.ecommerce.cart.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product line of a cart. Name and price are copied from the catalog when the product is added,
 * so that the cart can be displayed without calling the product service.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItem {

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;

    void increaseQuantity(int additionalQuantity) {
        this.quantity += additionalQuantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
