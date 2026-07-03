package com.saltmarsh.web;

import com.saltmarsh.exception.BusinessException;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.security.SaltmarshUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

@ControllerAdvice
public class GlobalControllerAdvice {

    /** Relative path only: single leading slash, no protocol-relative //, no backslashes. */
    private static final Pattern SAFE_PATH = Pattern.compile("^/[A-Za-z0-9._~/-]*$");

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

    @ExceptionHandler(ForbiddenException.class)
    public String handleForbidden(ForbiddenException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    static String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            // Reject absolute URLs that would navigate off-origin via odd path forms.
            if (uri.getScheme() != null && uri.getHost() != null) {
                // Only allow same-origin path extraction; still validate path strictly.
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            if (path.contains("\\") || !SAFE_PATH.matcher(path).matches()) {
                return "/";
            }
            String query = uri.getRawQuery();
            if (query != null && (query.contains("//") || query.contains("\\"))) {
                return path;
            }
            return query == null ? path : path + "?" + query;
        } catch (Exception e) {
            return "/";
        }
    }
}
