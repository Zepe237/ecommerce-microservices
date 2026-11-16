package com.feusap.ecommerce.inventory.repository;

import com.feusap.ecommerce.inventory.domain.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {
}
