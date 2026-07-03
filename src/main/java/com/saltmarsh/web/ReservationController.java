package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.dto.ReservationRequest;
import com.saltmarsh.service.BerthService;
import com.saltmarsh.service.CurrentUserService;
import com.saltmarsh.service.ReservationService;
import com.saltmarsh.service.VesselService;
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
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final VesselService vesselService;
    private final BerthService berthService;
    private final CurrentUserService currentUserService;

    public ReservationController(ReservationService reservationService,
                                 VesselService vesselService,
                                 BerthService berthService,
                                 CurrentUserService currentUserService) {
        this.reservationService = reservationService;
        this.vesselService = vesselService;
        this.berthService = berthService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("reservations", reservationService.listFor(user));
        return "reservations/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        LocalDate start = LocalDate.now().plusDays(1);
        model.addAttribute("reservationRequest",
                new ReservationRequest(null, null, start, start.plusDays(3), null));
        model.addAttribute("vessels", vesselService.listFor(user));
        model.addAttribute("berths", berthService.listAll());
        return "reservations/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("reservationRequest") ReservationRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("vessels", vesselService.listFor(user));
            model.addAttribute("berths", berthService.listAll());
            return "reservations/form";
        }
        var saved = reservationService.create(request, user);
        redirectAttributes.addFlashAttribute("success",
                "Reservation #" + saved.getId() + " created (" + saved.getStatus() + ")");
        return "redirect:/reservations";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("reservation", reservationService.getVisible(id, user));
        return "reservations/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        reservationService.confirm(id, user);
        redirectAttributes.addFlashAttribute("success", "Reservation confirmed");
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        reservationService.checkIn(id, user);
        redirectAttributes.addFlashAttribute("success", "Vessel checked in");
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/check-out")
    public String checkOut(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        reservationService.checkOut(id, user);
        redirectAttributes.addFlashAttribute("success", "Vessel checked out — invoice generated");
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/no-show")
    public String noShow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        reservationService.markNoShow(id, user);
        redirectAttributes.addFlashAttribute("success", "Marked as no-show; waitlist notified if applicable");
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        var reservation = reservationService.cancel(id, user);
        if (reservation.getLateCancelFee().signum() > 0) {
            redirectAttributes.addFlashAttribute("success",
                    "Reservation cancelled. Late fee: $" + reservation.getLateCancelFee());
        } else {
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled with no fee");
        }
        return "redirect:/reservations/" + id;
    }
}
