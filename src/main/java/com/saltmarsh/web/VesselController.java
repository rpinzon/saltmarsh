package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.Role;
import com.saltmarsh.domain.enums.VesselType;
import com.saltmarsh.dto.VesselRequest;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.service.CurrentUserService;
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

import java.util.List;

@Controller
@RequestMapping("/vessels")
public class VesselController {

    private final VesselService vesselService;
    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;

    public VesselController(VesselService vesselService,
                            CurrentUserService currentUserService,
                            UserAccountRepository userAccountRepository) {
        this.vesselService = vesselService;
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public String list(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("vessels", vesselService.listFor(user));
        return "vessels/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("vesselRequest", new VesselRequest("", "", null, null, null, "SAIL", null));
        model.addAttribute("vesselTypes", VesselType.values());
        addOwnersIfStaff(model);
        return "vessels/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("vesselRequest") VesselRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("vesselTypes", VesselType.values());
            addOwnersIfStaff(model);
            return "vessels/form";
        }
        UserAccount user = currentUserService.requireCurrentUser();
        vesselService.register(request, user);
        redirectAttributes.addFlashAttribute("success", "Vessel registered");
        return "redirect:/vessels";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        var vessel = vesselService.getVisible(id, user);
        model.addAttribute("vesselId", id);
        model.addAttribute("vesselRequest", new VesselRequest(
                vessel.getName(),
                vessel.getRegistrationNumber(),
                vessel.getLengthFeet(),
                vessel.getBeamFeet(),
                vessel.getDraftFeet(),
                vessel.getVesselType().name(),
                vessel.getOwner().getId()
        ));
        model.addAttribute("vesselTypes", VesselType.values());
        addOwnersIfStaff(model);
        return "vessels/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("vesselRequest") VesselRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("vesselId", id);
            model.addAttribute("vesselTypes", VesselType.values());
            addOwnersIfStaff(model);
            return "vessels/form";
        }
        UserAccount user = currentUserService.requireCurrentUser();
        vesselService.update(id, request, user);
        redirectAttributes.addFlashAttribute("success", "Vessel updated");
        return "redirect:/vessels";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        vesselService.deactivate(id, user);
        redirectAttributes.addFlashAttribute("success", "Vessel deactivated");
        return "redirect:/vessels";
    }

    private void addOwnersIfStaff(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (user.getRole().isStaffOrAbove()) {
            List<UserAccount> boaters = userAccountRepository.findByRoleInAndEnabledTrueOrderByFullNameAsc(
                    List.of(Role.BOATER));
            model.addAttribute("boaters", boaters);
        }
    }
}
