package com.feusap.ecommerce.cart.repository;

import com.feusap.ecommerce.cart.domain.Cart;
import org.springframework.data.repository.CrudRepository;

public interface CartRepository extends CrudRepository<Cart, Long> {
}
