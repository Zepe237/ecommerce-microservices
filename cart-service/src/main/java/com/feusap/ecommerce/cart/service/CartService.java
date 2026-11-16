package com.feusap.ecommerce.cart.service;

import com.feusap.ecommerce.cart.client.ProductClient;
import com.feusap.ecommerce.cart.client.dto.ProductResponse;
import com.feusap.ecommerce.cart.domain.Cart;
import com.feusap.ecommerce.cart.dto.CartResponse;
import com.feusap.ecommerce.cart.exception.ProductNotFoundException;
import com.feusap.ecommerce.cart.mapper.CartMapper;
import com.feusap.ecommerce.cart.repository.CartRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final CartMapper cartMapper;

    /**
     * Returns the customer's cart, or an empty cart when none exists yet.
     */
    public CartResponse getCart(Long customerId) {
        return cartMapper.toResponse(loadOrCreate(customerId));
    }

    public CartResponse addItem(Long customerId, String productId, int quantity) {
        ProductResponse product = fetchProduct(productId);

        Cart cart = loadOrCreate(customerId);
        cart.addItem(product.id(), product.name(), product.price(), quantity);
        Cart saved = cartRepository.save(cart);

        log.info("Added {} x product {} to the cart of customer {}", quantity, productId, customerId);
        return cartMapper.toResponse(saved);
    }

    public CartResponse removeItem(Long customerId, String productId) {
        Cart cart = loadOrCreate(customerId);
        cart.removeItem(productId);
        Cart saved = cartRepository.save(cart);

        log.info("Removed product {} from the cart of customer {}", productId, customerId);
        return cartMapper.toResponse(saved);
    }

    public void clear(Long customerId) {
        cartRepository.deleteById(customerId);
        log.info("Cleared the cart of customer {}", customerId);
    }

    private Cart loadOrCreate(Long customerId) {
        return cartRepository.findById(customerId).orElseGet(() -> new Cart(customerId));
    }

    private ProductResponse fetchProduct(String productId) {
        try {
            return productClient.getProduct(productId);
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException(productId);
        }
    }
}
