package com.saltmarsh.web;

import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.security.SaltmarshUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addUser(@AuthenticationPrincipal SaltmarshUserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("currentUser", user);
            model.addAttribute("currentRole", user.getRole().name());
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException ex, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    private String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.isBlank() || !path.startsWith("/")) {
                return "/";
            }
            String query = uri.getQuery();
            return query == null ? path : path + "?" + query;
        } catch (Exception e) {
            return "/";
        }
    }
}
