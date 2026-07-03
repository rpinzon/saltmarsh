package com.saltmarsh.domain;

import com.saltmarsh.domain.enums.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderStatusTest {

    @Test
    void openFlowsToAssignedOrCancelled() {
        assertTrue(WorkOrderStatus.OPEN.canTransitionTo(WorkOrderStatus.ASSIGNED));
        assertTrue(WorkOrderStatus.OPEN.canTransitionTo(WorkOrderStatus.CANCELLED));
        assertFalse(WorkOrderStatus.OPEN.canTransitionTo(WorkOrderStatus.COMPLETED));
    }

    @Test
    void inProgressCanCompleteOrBlock() {
        assertTrue(WorkOrderStatus.IN_PROGRESS.canTransitionTo(WorkOrderStatus.COMPLETED));
        assertTrue(WorkOrderStatus.IN_PROGRESS.canTransitionTo(WorkOrderStatus.BLOCKED));
        assertFalse(WorkOrderStatus.IN_PROGRESS.canTransitionTo(WorkOrderStatus.OPEN));
    }

    @Test
    void completedIsTerminal() {
        assertTrue(WorkOrderStatus.COMPLETED.isTerminal());
        assertFalse(WorkOrderStatus.COMPLETED.canTransitionTo(WorkOrderStatus.IN_PROGRESS));
    }
}
