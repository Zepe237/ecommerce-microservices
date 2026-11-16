package com.feusap.ecommerce.cart.controller;

import com.feusap.ecommerce.cart.dto.AddItemRequest;
import com.feusap.ecommerce.cart.dto.CartResponse;
import com.feusap.ecommerce.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts/{customerId}")
@RequiredArgsConstructor
@Tag(name = "Carts", description = "Customer shopping carts")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the cart of a customer",
            description = "Returns an empty cart when the customer has not added anything yet.")
    public CartResponse getCart(@PathVariable Long customerId) {
        return cartService.getCart(customerId);
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart",
            description = "Increases the quantity when the product is already in the cart.")
    public CartResponse addItem(@PathVariable Long customerId, @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(customerId, request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the cart")
    public CartResponse removeItem(@PathVariable Long customerId, @PathVariable String productId) {
        return cartService.removeItem(customerId, productId);
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<Void> clear(@PathVariable Long customerId) {
        cartService.clear(customerId);
        return ResponseEntity.noContent().build();
    }
}
