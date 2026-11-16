package com.feusap.ecommerce.product.service;

import com.feusap.ecommerce.product.domain.Product;
import com.feusap.ecommerce.product.dto.ProductRequest;
import com.feusap.ecommerce.product.dto.ProductResponse;
import com.feusap.ecommerce.product.exception.ProductNotFoundException;
import com.feusap.ecommerce.product.mapper.ProductMapper;
import com.feusap.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse findById(String productId) {
        return productRepository.findById(productId)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public ProductResponse create(ProductRequest request) {
        Product saved = productRepository.save(productMapper.toDocument(request));
        log.info("Product {} created", saved.id());
        return productMapper.toResponse(saved);
    }
}
