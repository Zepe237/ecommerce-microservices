package com.feusap.ecommerce.cart.service;

import com.feusap.ecommerce.cart.client.ProductClient;
import com.feusap.ecommerce.cart.client.dto.ProductResponse;
import com.feusap.ecommerce.cart.domain.Cart;
import com.feusap.ecommerce.cart.dto.CartResponse;
import com.feusap.ecommerce.cart.mapper.CartMapper;
import com.feusap.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long CUSTOMER_ID = 1L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductClient productClient;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productClient, new CartMapper());
    }

    @Test
    @DisplayName("getCart returns an empty cart when the customer has none")
    void getCart_returnsEmptyCart() {
        when(cartRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        CartResponse cart = cartService.getCart(CUSTOMER_ID);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("addItem merges quantities when the product is already in the cart")
    void addItem_mergesQuantities() {
        Cart existing = new Cart(CUSTOMER_ID);
        existing.addItem("PRD-0001", "iPhone 15", new BigDecimal("1000.00"), 1);
        when(cartRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(productClient.getProduct("PRD-0001"))
                .thenReturn(new ProductResponse("PRD-0001", "iPhone 15", new BigDecimal("1000.00")));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse cart = cartService.addItem(CUSTOMER_ID, "PRD-0001", 2);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
        assertThat(cart.totalAmount()).isEqualByComparingTo("3000.00");
    }
}
