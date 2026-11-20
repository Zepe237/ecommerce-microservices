package com.feusap.ecommerce.inventory.repository;

import com.feusap.ecommerce.inventory.domain.ReservationStatus;
import com.feusap.ecommerce.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    boolean existsByOrderId(Long orderId);

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
