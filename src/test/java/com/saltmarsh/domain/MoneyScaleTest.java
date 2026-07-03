package com.saltmarsh.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyScaleTest {

    @Test
    void lineItemRoundsToTwoDecimals() {
        InvoiceLineItem item = InvoiceLineItem.of("Labor", new BigDecimal("1.33"), new BigDecimal("85.00"));
        assertEquals(new BigDecimal("113.05"), item.getLineTotal());
        assertEquals(2, item.getLineTotal().scale());
    }

    @Test
    void reservationTotalRoundsToTwoDecimals() {
        BigDecimal total = Reservation.calculateTotal(
                new BigDecimal("85.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4));
        assertEquals(new BigDecimal("255.00"), total);
        assertEquals(2, total.scale());
    }
}
