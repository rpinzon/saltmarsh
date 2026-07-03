package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.BerthStatus;
import com.saltmarsh.domain.enums.BerthType;
import com.saltmarsh.dto.BerthRequest;
import com.saltmarsh.service.BerthService;
import com.saltmarsh.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/berths")
public class BerthController {

    private final BerthService berthService;
    private final CurrentUserService currentUserService;

    public BerthController(BerthService berthService, CurrentUserService currentUserService) {
        this.berthService = berthService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(required = false) BigDecimal lengthFeet,
                       @RequestParam(required = false) BigDecimal draftFeet,
                       Model model) {
        model.addAttribute("berths", berthService.listAll());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("lengthFeet", lengthFeet);
        model.addAttribute("draftFeet", draftFeet);
        if (startDate != null && endDate != null && lengthFeet != null && draftFeet != null) {
            model.addAttribute("available", berthService.findAvailable(startDate, endDate, lengthFeet, draftFeet));
        }
        return "berths/list";
    }

    @GetMapping("/manage/new")
    @PreAuthorize("hasAnyRole('HARBORMASTER','ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("berthRequest", new BerthRequest("", "", null, null, "TRANSIENT", null, "AVAILABLE", null));
        model.addAttribute("berthTypes", BerthType.values());
        model.addAttribute("berthStatuses", BerthStatus.values());
        return "berths/form";
    }

    @PostMapping("/manage")
    @PreAuthorize("hasAnyRole('HARBORMASTER','ADMIN')")
    public String create(@Valid @ModelAttribute("berthRequest") BerthRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("berthTypes", BerthType.values());
            model.addAttribute("berthStatuses", BerthStatus.values());
            return "berths/form";
        }
        UserAccount user = currentUserService.requireCurrentUser();
        berthService.create(request, user);
        redirectAttributes.addFlashAttribute("success", "Berth created");
        return "redirect:/berths";
    }

    @GetMapping("/manage/{id}/edit")
    @PreAuthorize("hasAnyRole('HARBORMASTER','ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        var berth = berthService.get(id);
        model.addAttribute("berthId", id);
        model.addAttribute("berthRequest", new BerthRequest(
                berth.getCode(), berth.getPier(), berth.getMaxLengthFeet(), berth.getMaxDraftFeet(),
                berth.getBerthType().name(), berth.getDailyRate(), berth.getStatus().name(), berth.getNotes()
        ));
        model.addAttribute("berthTypes", BerthType.values());
        model.addAttribute("berthStatuses", BerthStatus.values());
        return "berths/form";
    }

    @PostMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('HARBORMASTER','ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("berthRequest") BerthRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("berthId", id);
            model.addAttribute("berthTypes", BerthType.values());
            model.addAttribute("berthStatuses", BerthStatus.values());
            return "berths/form";
        }
        UserAccount user = currentUserService.requireCurrentUser();
        berthService.update(id, request, user);
        redirectAttributes.addFlashAttribute("success", "Berth updated");
        return "redirect:/berths";
    }

    @PostMapping("/manage/{id}/status")
    @PreAuthorize("hasAnyRole('HARBORMASTER','ADMIN')")
    public String status(@PathVariable Long id,
                         @RequestParam BerthStatus status,
                         RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        berthService.updateStatus(id, status, user);
        redirectAttributes.addFlashAttribute("success", "Berth status updated to " + status);
        return "redirect:/berths";
    }
}
