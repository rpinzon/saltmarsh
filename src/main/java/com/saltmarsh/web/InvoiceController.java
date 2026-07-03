package com.saltmarsh.web;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.service.CurrentUserService;
import com.saltmarsh.service.InvoiceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CurrentUserService currentUserService;

    public InvoiceController(InvoiceService invoiceService, CurrentUserService currentUserService) {
        this.invoiceService = invoiceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("invoices", invoiceService.listFor(user));
        return "invoices/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        UserAccount user = currentUserService.requireCurrentUser();
        model.addAttribute("invoice", invoiceService.getVisible(id, user));
        return "invoices/detail";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        invoiceService.markPaid(id, user);
        redirectAttributes.addFlashAttribute("success", "Invoice marked as paid");
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UserAccount user = currentUserService.requireCurrentUser();
        invoiceService.voidInvoice(id, user);
        redirectAttributes.addFlashAttribute("success", "Invoice voided");
        return "redirect:/invoices/" + id;
    }
}
