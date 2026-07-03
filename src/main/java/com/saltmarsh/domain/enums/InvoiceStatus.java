package com.saltmarsh.domain.enums;

public enum InvoiceStatus {
    DRAFT, ISSUED, PAID, VOID;

    public boolean canTransitionTo(InvoiceStatus target) {
        return switch (this) {
            case DRAFT -> target == ISSUED || target == VOID;
            case ISSUED -> target == PAID || target == VOID;
            case PAID, VOID -> false;
        };
    }
}
