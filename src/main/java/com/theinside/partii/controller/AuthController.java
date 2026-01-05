package com.theinside.partii.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
            System.out.println(principal);
            log.info("{} Accessed /user endpoint", principal.getName());
            return "Hi " + principal.getName() + ", you're an authorised user";
        }
        return "Hi, you're an authorised user";
    }

    //mockup logout endpoint
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        log.info("User logged out, redirecting to login");

        response.sendRedirect("/login");
        return null;
    }
}
