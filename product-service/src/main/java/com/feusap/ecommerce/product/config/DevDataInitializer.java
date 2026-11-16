package com.feusap.ecommerce.product.config;

import com.feusap.ecommerce.product.domain.Product;
import com.feusap.ecommerce.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the catalog for local development. Identifiers are fixed so that the
 * inventory service can seed matching stock levels (see its DevDataInitializer).
 */
@Slf4j
@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    ApplicationRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                log.info("Catalog already populated, skipping seed");
                return;
            }
            List<Product> products = List.of(
                    new Product("PRD-0001", "iPhone 15", "Apple smartphone, 128 GB", new BigDecimal("1099.99")),
                    new Product("PRD-0002", "Galaxy S24", "Samsung smartphone, 256 GB", new BigDecimal("999.99")),
                    new Product("PRD-0003", "MacBook Air M3", "13-inch laptop, 16 GB RAM", new BigDecimal("1499.00")),
                    new Product("PRD-0004", "AirPods Pro", "Noise-cancelling earbuds", new BigDecimal("329.99")),
                    new Product("PRD-0005", "Logitech MX Master 3S", "Ergonomic wireless mouse", new BigDecimal("129.99"))
            );
            productRepository.saveAll(products);
            log.info("Seeded {} products", products.size());
        };
    }
}
