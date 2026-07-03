package com.saltmarsh.service;

import com.saltmarsh.domain.Berth;
import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.Vessel;
import com.saltmarsh.domain.WorkOrder;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.WorkOrderCategory;
import com.saltmarsh.domain.enums.WorkOrderPriority;
import com.saltmarsh.domain.enums.WorkOrderStatus;
import com.saltmarsh.dto.WorkOrderAssignRequest;
import com.saltmarsh.dto.WorkOrderCompleteRequest;
import com.saltmarsh.dto.WorkOrderRequest;
import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.BerthRepository;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.repository.VesselRepository;
import com.saltmarsh.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final VesselRepository vesselRepository;
    private final BerthRepository berthRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final InvoiceService invoiceService;
    private final Clock clock;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            VesselRepository vesselRepository,
                            BerthRepository berthRepository,
                            UserAccountRepository userAccountRepository,
                            AuditService auditService,
                            InvoiceService invoiceService,
                            Clock clock) {
        this.workOrderRepository = workOrderRepository;
        this.vesselRepository = vesselRepository;
        this.berthRepository = berthRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.invoiceService = invoiceService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> listFor(UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return workOrderRepository.findAllDetailed();
        }
        return workOrderRepository.findVisibleToBoater(actor.getId());
    }

    @Transactional(readOnly = true)
    public WorkOrder getVisible(Long id, UserAccount actor) {
        WorkOrder workOrder = workOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found"));
        assertCanView(workOrder, actor);
        return workOrder;
    }

    @Transactional
    public WorkOrder create(WorkOrderRequest request, UserAccount actor) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(request.title().trim());
        workOrder.setDescription(request.description().trim());
        workOrder.setPriority(WorkOrderPriority.valueOf(request.priority()));
        workOrder.setCategory(WorkOrderCategory.valueOf(request.category()));
        workOrder.setStatus(WorkOrderStatus.OPEN);
        workOrder.setReportedBy(actor);

        if (request.vesselId() != null) {
            Vessel vessel = vesselRepository.findByIdWithOwner(request.vesselId())
                    .orElseThrow(() -> new NotFoundException("Vessel not found"));
            if (!actor.getRole().isStaffOrAbove() && !vessel.getOwner().getId().equals(actor.getId())) {
                throw new ForbiddenException("You can only open work orders for your vessels");
            }
            workOrder.setVessel(vessel);
        }
        if (request.berthId() != null) {
            Berth berth = berthRepository.findById(request.berthId())
                    .orElseThrow(() -> new NotFoundException("Berth not found"));
            workOrder.setBerth(berth);
        }
        if (request.dueAt() != null) {
            workOrder.setDueAt(request.dueAt());
        }

        WorkOrder saved = workOrderRepository.save(workOrder);
        auditService.record(actor, "WORK_ORDER_CREATED", "WorkOrder", saved.getId(),
                saved.getPriority() + " / " + saved.getCategory() + ": " + saved.getTitle());
        return workOrderRepository.findDetailedById(saved.getId()).orElse(saved);
    }

    @Transactional
    public WorkOrder assign(Long id, WorkOrderAssignRequest request, UserAccount actor) {
        requireStaff(actor);
        WorkOrder workOrder = getVisible(id, actor);
        if (workOrder.getStatus().isTerminal()) {
            throw new BusinessException("TERMINAL", "Cannot assign a terminal work order");
        }

        UserAccount assignee = userAccountRepository.findById(request.assigneeId())
                .orElseThrow(() -> new NotFoundException("Assignee not found"));
        if (!assignee.getRole().isStaffOrAbove()) {
            throw new BusinessException("INVALID_ASSIGNEE", "Assignee must be staff or above");
        }

        workOrder.setAssignedTo(assignee);
        if (workOrder.getStatus() == WorkOrderStatus.OPEN || workOrder.getStatus() == WorkOrderStatus.BLOCKED) {
            if (!workOrder.getStatus().canTransitionTo(WorkOrderStatus.ASSIGNED)
                    && workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
                // OPEN -> ASSIGNED is valid; BLOCKED -> ASSIGNED is valid
            }
            if (workOrder.getStatus() != WorkOrderStatus.ASSIGNED
                    && workOrder.getStatus().canTransitionTo(WorkOrderStatus.ASSIGNED)) {
                workOrder.setStatus(WorkOrderStatus.ASSIGNED);
            } else if (workOrder.getStatus() == WorkOrderStatus.OPEN) {
                workOrder.setStatus(WorkOrderStatus.ASSIGNED);
            }
        }

        auditService.record(actor, "WORK_ORDER_ASSIGNED", "WorkOrder", workOrder.getId(),
                "Assigned to " + assignee.getFullName());
        return workOrder;
    }

    @Transactional
    public WorkOrder start(Long id, UserAccount actor) {
        requireStaff(actor);
        WorkOrder workOrder = getVisible(id, actor);
        if (workOrder.getAssignedTo() == null) {
            workOrder.setAssignedTo(actor);
            if (workOrder.getStatus() == WorkOrderStatus.OPEN) {
                workOrder.setStatus(WorkOrderStatus.ASSIGNED);
            }
        }
        transition(workOrder, WorkOrderStatus.IN_PROGRESS, actor, "WORK_ORDER_STARTED");
        if (workOrder.getStartedAt() == null) {
            workOrder.setStartedAt(Instant.now(clock));
        }
        return workOrder;
    }

    @Transactional
    public WorkOrder block(Long id, String reason, UserAccount actor) {
        requireStaff(actor);
        WorkOrder workOrder = getVisible(id, actor);
        transition(workOrder, WorkOrderStatus.BLOCKED, actor, "WORK_ORDER_BLOCKED");
        auditService.record(actor, "WORK_ORDER_BLOCK_REASON", "WorkOrder", workOrder.getId(), reason);
        return workOrder;
    }

    @Transactional
    public WorkOrder complete(Long id, WorkOrderCompleteRequest request, UserAccount actor) {
        requireStaff(actor);
        WorkOrder workOrder = getVisible(id, actor);
        workOrder.setLaborHours(request.laborHours());
        if (request.partsCost() != null) {
            workOrder.setPartsCost(request.partsCost());
        }
        workOrder.setCompletedAt(Instant.now(clock));
        transition(workOrder, WorkOrderStatus.COMPLETED, actor, "WORK_ORDER_COMPLETED");

        if (workOrder.getVessel() != null) {
            invoiceService.createFromWorkOrder(workOrder, actor);
        }
        return workOrder;
    }

    @Transactional
    public WorkOrder cancel(Long id, UserAccount actor) {
        WorkOrder workOrder = getVisible(id, actor);
        boolean reporter = workOrder.getReportedBy().getId().equals(actor.getId());
        if (!reporter && !actor.getRole().isStaffOrAbove()) {
            throw new ForbiddenException("You cannot cancel this work order");
        }
        transition(workOrder, WorkOrderStatus.CANCELLED, actor, "WORK_ORDER_CANCELLED");
        return workOrder;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> staffMembers() {
        return userAccountRepository.findByRoleInAndEnabledTrueOrderByFullNameAsc(
                List.of(Role.STAFF, Role.HARBORMASTER, Role.ADMIN));
    }

    private void transition(WorkOrder workOrder, WorkOrderStatus target, UserAccount actor, String action) {
        if (!workOrder.getStatus().canTransitionTo(target)) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Cannot transition from " + workOrder.getStatus() + " to " + target);
        }
        workOrder.setStatus(target);
        auditService.record(actor, action, "WorkOrder", workOrder.getId(), "Status → " + target);
    }

    private void assertCanView(WorkOrder workOrder, UserAccount actor) {
        if (actor.getRole().isStaffOrAbove()) {
            return;
        }
        boolean ownsVessel = workOrder.getVessel() != null
                && workOrder.getVessel().getOwner().getId().equals(actor.getId());
        boolean isReporter = workOrder.getReportedBy().getId().equals(actor.getId());
        if (!ownsVessel && !isReporter) {
            throw new ForbiddenException("You cannot view this work order");
        }
    }

    private void requireStaff(UserAccount actor) {
        if (!actor.getRole().isStaffOrAbove()) {
            throw new ForbiddenException("Staff access required");
        }
    }
}
