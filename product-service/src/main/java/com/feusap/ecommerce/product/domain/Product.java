package com.feusap.ecommerce.product.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * Catalog entry. Stock levels are owned by the inventory service, not by the catalog.
 */
@Document(collection = "products")
public record Product(
        @Id String id,
        String name,
        String description,
        BigDecimal price
) {
}
