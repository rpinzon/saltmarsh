package com.saltmarsh.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    NO_SHOW;

    public static final Set<ReservationStatus> ACTIVE_OCCUPYING = EnumSet.of(CONFIRMED, CHECKED_IN, PENDING);

    public boolean occupiesBerth() {
        return ACTIVE_OCCUPYING.contains(this);
    }

    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CHECKED_IN || target == CANCELLED || target == NO_SHOW;
            case CHECKED_IN -> target == CHECKED_OUT;
            case CHECKED_OUT, CANCELLED, NO_SHOW -> false;
        };
    }
}
