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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public List<ProductResponse> search(String query, BigDecimal maxPrice) {
        String keyword = query.toLowerCase();
        return productRepository.findAll().stream()
            .filter(p -> p.name().toLowerCase().contains(keyword)
                || p.description().toLowerCase().contains(keyword))
            .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
            .map(productMapper::toResponse)
            .toList();
    }
}
