package com.feusap.ecommerce.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    @Test
    @DisplayName("A new reservation starts as RESERVED")
    void startsReserved() {
        assertThat(new StockReservation(1L, "PRD-0001", 2).getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("A committed reservation cannot be released")
    void committedCannotBeReleased() {
        StockReservation reservation = new StockReservation(1L, "PRD-0001", 2);
        reservation.commit();

        assertThatThrownBy(reservation::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A released reservation cannot be committed")
    void releasedCannotBeCommitted() {
        StockReservation reservation = new StockReservation(1L, "PRD-0001", 2);
        reservation.release();

        assertThatThrownBy(reservation::commit).isInstanceOf(IllegalStateException.class);
    }
}
