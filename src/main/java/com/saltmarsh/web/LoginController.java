package com.saltmarsh.web;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final Environment environment;

    public LoginController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been signed out");
        }
        // Never advertise demo credentials outside the local developer profile.
        model.addAttribute("showDemoAccounts", environment.matchesProfiles("local"));
        return "login";
    }
}
