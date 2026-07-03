package com.saltmarsh.domain.enums;

public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(WorkOrderStatus target) {
        return switch (this) {
            case OPEN -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == IN_PROGRESS || target == OPEN || target == CANCELLED || target == BLOCKED;
            case IN_PROGRESS -> target == BLOCKED || target == COMPLETED || target == CANCELLED;
            case BLOCKED -> target == IN_PROGRESS || target == ASSIGNED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
