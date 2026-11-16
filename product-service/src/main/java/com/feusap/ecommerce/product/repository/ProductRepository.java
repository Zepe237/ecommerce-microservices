package com.feusap.ecommerce.product.repository;

import com.feusap.ecommerce.product.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
