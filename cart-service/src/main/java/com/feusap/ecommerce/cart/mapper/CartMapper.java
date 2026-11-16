package com.feusap.ecommerce.cart.mapper;

import com.feusap.ecommerce.cart.domain.Cart;
import com.feusap.ecommerce.cart.domain.CartItem;
import com.feusap.ecommerce.cart.dto.CartItemResponse;
import com.feusap.ecommerce.cart.dto.CartResponse;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.getCustomerId(),
                cart.getItems().stream().map(this::toResponse).toList(),
                cart.getTotalAmount());
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }
}
