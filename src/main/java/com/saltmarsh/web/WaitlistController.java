package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.dto.WaitlistRequest;
import com.saltmarsh.service.CurrentUserService;
import com.saltmarsh.service.VesselService;
import com.saltmarsh.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final VesselService vesselService;
    private final CurrentUserService currentUserService;

    public WaitlistController(WaitlistService waitlistService,
                              VesselService vesselService,
                              CurrentUserService currentUserService) {
        this.waitlistService = waitlistService;
        this.vesselService = vesselService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (user.getRole().isStaffOrAbove()) {
            model.addAttribute("entries", waitlistService.listAllActive());
        } else {
            model.addAttribute("entries", waitlistService.listFor(user));
        }
        return "waitlist/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        LocalDate start = LocalDate.now().plusDays(7);
        model.addAttribute("waitlistRequest",
                new WaitlistRequest(null, start, start.plusDays(5), null, null));
        model.addAttribute("vessels", vesselService.listFor(user));
        return "waitlist/form";
    }

    @PostMapping
    public String join(@Valid @ModelAttribute("waitlistRequest") WaitlistRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("vessels", vesselService.listFor(user));
            return "waitlist/form";
        }
        waitlistService.join(request, user);
        redirectAttributes.addFlashAttribute("success", "Added to waitlist");
        return "redirect:/waitlist";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        var reservation = waitlistService.acceptOffer(id, user);
        redirectAttributes.addFlashAttribute("success",
                "Offer accepted — reservation #" + reservation.getId() + " confirmed");
        return "redirect:/reservations/" + reservation.getId();
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        waitlistService.cancel(id, user);
        redirectAttributes.addFlashAttribute("success", "Waitlist entry cancelled");
        return "redirect:/waitlist";
    }
}
