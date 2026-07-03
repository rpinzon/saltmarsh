package com.saltmarsh.domain.enums;

public enum Role {
    BOATER,
    STAFF,
    HARBORMASTER,
    ADMIN;

    public boolean isStaffOrAbove() {
        return this == STAFF || this == HARBORMASTER || this == ADMIN;
    }

    public boolean isHarbormasterOrAbove() {
        return this == HARBORMASTER || this == ADMIN;
    }
}
