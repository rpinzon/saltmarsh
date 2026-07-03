package com.saltmarsh.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselFitTest {

    @Test
    void fitsWhenWithinLengthAndDraft() {
        Vessel vessel = new Vessel();
        vessel.setLengthFeet(new BigDecimal("30.00"));
        vessel.setDraftFeet(new BigDecimal("4.00"));

        Berth berth = new Berth();
        berth.setMaxLengthFeet(new BigDecimal("35.00"));
        berth.setMaxDraftFeet(new BigDecimal("6.00"));

        assertTrue(vessel.fitsIn(berth));
    }

    @Test
    void rejectsWhenTooLongOrDeep() {
        Vessel vessel = new Vessel();
        vessel.setLengthFeet(new BigDecimal("40.00"));
        vessel.setDraftFeet(new BigDecimal("4.00"));

        Berth berth = new Berth();
        berth.setMaxLengthFeet(new BigDecimal("35.00"));
        berth.setMaxDraftFeet(new BigDecimal("6.00"));

        assertFalse(vessel.fitsIn(berth));

        vessel.setLengthFeet(new BigDecimal("30.00"));
        vessel.setDraftFeet(new BigDecimal("7.00"));
        assertFalse(vessel.fitsIn(berth));
    }
}
