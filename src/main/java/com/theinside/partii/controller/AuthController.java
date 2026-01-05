package com.theinside.partii.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Slf4j
@RequestMapping("/partii/api/v1/auth")
public class AuthController {
    @GetMapping("/home")
    public String home() {
        return "Home";
    }

    @GetMapping("/user")
    public String user(Principal principal) {
        if (principal != null) {
            return "Hi " + principal.getName() + ", you're an authorised user";
        }
        return "Hi, you're an authorised user";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "You have been logged out successfully. Visit /home to return to the home page.";
    }
}
