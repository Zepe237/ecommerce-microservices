package com.feusap.ecommerce.product.mapper;

import com.feusap.ecommerce.product.domain.Product;
import com.feusap.ecommerce.product.dto.ProductRequest;
import com.feusap.ecommerce.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(product.id(), product.name(), product.description(), product.price());
    }

    public Product toDocument(ProductRequest request) {
        return new Product(null, request.name(), request.description(), request.price());
    }
}
