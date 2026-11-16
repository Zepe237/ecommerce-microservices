package com.feusap.ecommerce.product.service;

import com.feusap.ecommerce.product.domain.Product;
import com.feusap.ecommerce.product.dto.ProductRequest;
import com.feusap.ecommerce.product.dto.ProductResponse;
import com.feusap.ecommerce.product.exception.ProductNotFoundException;
import com.feusap.ecommerce.product.mapper.ProductMapper;
import com.feusap.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, new ProductMapper());
    }

    @Test
    @DisplayName("findAll maps every stored product to a response")
    void findAll_mapsProducts() {
        when(productRepository.findAll()).thenReturn(List.of(
                new Product("PRD-0001", "iPhone 15", "Apple smartphone", new BigDecimal("1099.99")),
                new Product("PRD-0002", "Galaxy S24", "Samsung smartphone", new BigDecimal("999.99"))
        ));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).extracting(ProductResponse::name).containsExactly("iPhone 15", "Galaxy S24");
        assertThat(result.getFirst().price()).isEqualByComparingTo("1099.99");
    }

    @Test
    @DisplayName("findById throws when the product does not exist")
    void findById_throwsWhenMissing() {
        when(productRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("unknown"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("create persists the product and returns the generated identifier")
    void create_returnsGeneratedId() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product toSave = invocation.getArgument(0);
                    return new Product("generated-id", toSave.name(), toSave.description(), toSave.price());
                });

        ProductResponse created = productService.create(
                new ProductRequest("Kindle", "Amazon e-reader", new BigDecimal("159.99")));

        assertThat(created.id()).isEqualTo("generated-id");
        assertThat(created.name()).isEqualTo("Kindle");
    }
}
