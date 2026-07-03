package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationStatusTest {

    @Test
    void pendingCanConfirmOrCancel() {
        assertTrue(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CONFIRMED));
        assertTrue(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CANCELLED));
        assertFalse(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CHECKED_IN));
    }

    @Test
    void confirmedCanCheckInCancelOrNoShow() {
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CHECKED_IN));
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CANCELLED));
        assertTrue(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.NO_SHOW));
        assertFalse(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CHECKED_OUT));
    }

    @Test
    void terminalStatesAreTerminal() {
        assertFalse(ReservationStatus.CHECKED_OUT.canTransitionTo(ReservationStatus.CANCELLED));
        assertFalse(ReservationStatus.CANCELLED.canTransitionTo(ReservationStatus.CONFIRMED));
        assertFalse(ReservationStatus.NO_SHOW.canTransitionTo(ReservationStatus.CHECKED_IN));
    }

    @Test
    void calculateTotalUsesNightsNotInclusiveDays() {
        BigDecimal total = Reservation.calculateTotal(
                new BigDecimal("100.00"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 4));
        assertEquals(new BigDecimal("300.00"), total);
    }
}
