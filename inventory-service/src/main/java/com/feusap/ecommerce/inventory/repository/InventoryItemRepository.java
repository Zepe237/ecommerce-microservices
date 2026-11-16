package com.feusap.ecommerce.inventory.repository;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Deducts stock atomically. Returns 0 when the product is unknown or the stock is insufficient.
     */
    @Modifying
    @Query("""
            update InventoryItem i
               set i.quantity = i.quantity - :quantity, i.version = i.version + 1
             where i.productId = :productId
               and i.quantity - i.reserved >= :quantity
            """)
    int deduct(@Param("productId") String productId, @Param("quantity") int quantity);
}
