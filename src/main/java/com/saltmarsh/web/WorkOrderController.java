package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.WorkOrderCategory;
import com.saltmarsh.domain.enums.WorkOrderPriority;
import com.saltmarsh.dto.WorkOrderAssignRequest;
import com.saltmarsh.dto.WorkOrderCompleteRequest;
import com.saltmarsh.dto.WorkOrderRequest;
import com.saltmarsh.service.BerthService;
import com.saltmarsh.service.CurrentUserService;
import com.saltmarsh.service.VesselService;
import com.saltmarsh.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final VesselService vesselService;
    private final BerthService berthService;
    private final CurrentUserService currentUserService;

    public WorkOrderController(WorkOrderService workOrderService,
                               VesselService vesselService,
                               BerthService berthService,
                               CurrentUserService currentUserService) {
        this.workOrderService = workOrderService;
        this.vesselService = vesselService;
        this.berthService = berthService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("workOrders", workOrderService.listFor(user));
        return "workorders/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("workOrderRequest",
                new WorkOrderRequest("", "", "MEDIUM", "OTHER", null, null, null));
        model.addAttribute("priorities", WorkOrderPriority.values());
        model.addAttribute("categories", WorkOrderCategory.values());
        model.addAttribute("vessels", vesselService.listFor(user));
        model.addAttribute("berths", berthService.listAll());
        return "workorders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("workOrderRequest") WorkOrderRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("priorities", WorkOrderPriority.values());
            model.addAttribute("categories", WorkOrderCategory.values());
            model.addAttribute("vessels", vesselService.listFor(user));
            model.addAttribute("berths", berthService.listAll());
            return "workorders/form";
        }
        var saved = workOrderService.create(request, user);
        redirectAttributes.addFlashAttribute("success", "Work order #" + saved.getId() + " created");
        return "redirect:/work-orders/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("workOrder", workOrderService.getVisible(id, user));
        if (user.getRole().isStaffOrAbove()) {
            model.addAttribute("staffMembers", workOrderService.staffMembers());
            model.addAttribute("assignRequest", new WorkOrderAssignRequest(null));
            model.addAttribute("completeRequest", new WorkOrderCompleteRequest(null, null));
        }
        return "workorders/detail";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id,
                         @Valid @ModelAttribute("assignRequest") WorkOrderAssignRequest request,
                         RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        workOrderService.assign(id, request, user);
        redirectAttributes.addFlashAttribute("success", "Work order assigned");
        return "redirect:/work-orders/" + id;
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        workOrderService.start(id, user);
        redirectAttributes.addFlashAttribute("success", "Work order started");
        return "redirect:/work-orders/" + id;
    }

    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id,
                        @RequestParam(defaultValue = "Blocked") String reason,
                        RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        workOrderService.block(id, reason, user);
        redirectAttributes.addFlashAttribute("success", "Work order blocked");
        return "redirect:/work-orders/" + id;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @Valid @ModelAttribute("completeRequest") WorkOrderCompleteRequest request,
                           RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        workOrderService.complete(id, request, user);
        redirectAttributes.addFlashAttribute("success", "Work order completed — invoice generated if billable");
        return "redirect:/work-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        workOrderService.cancel(id, user);
        redirectAttributes.addFlashAttribute("success", "Work order cancelled");
        return "redirect:/work-orders/" + id;
    }
}
