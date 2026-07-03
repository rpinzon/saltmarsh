package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.service.AuditService;
import com.saltmarsh.service.CurrentUserService;
import com.saltmarsh.service.DashboardService;
import com.saltmarsh.service.ReservationService;
import com.saltmarsh.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReservationService reservationService;
    private final WorkOrderService workOrderService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService,
                               ReservationService reservationService,
                               WorkOrderService workOrderService,
                               AuditService auditService,
                               CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.reservationService = reservationService;
        this.workOrderService = workOrderService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/")
    public String home(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("stats", dashboardService.stats());
        model.addAttribute("docked", reservationService.currentlyDocked());
        model.addAttribute("myReservations", reservationService.listFor(user).stream().limit(8).toList());
        model.addAttribute("workOrders", workOrderService.listFor(user).stream().limit(8).toList());
        if (user.getRole().isStaffOrAbove()) {
            model.addAttribute("auditEvents", auditService.recent().stream().limit(12).toList());
        }
        return "dashboard";
    }
}
