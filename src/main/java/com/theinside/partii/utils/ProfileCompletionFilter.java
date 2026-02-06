//package com.theinside.partii.utils;
//
//
//import com.theinside.partii.security.SecurityUser;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Set;
//
//@Component
//public class ProfileCompletionFilter implements Filter {
//
//    private static final Set<String> ALLOWED_PATHS = Set.of(
//            "/partii/api/v1/auth/logout",
//            "/partii/api/v1/users/complete-profile",
//            "/partii/api/v1/users/me"
//    );
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response,
//                         FilterChain chain) throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//
//        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof SecurityUser securityUser) {
//
//            if (!securityUser.isProfileComplete() &&
//                    !isAllowedPath(httpRequest.getRequestURI())) {
//
//                httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
//                httpResponse.setContentType("application/json");
//                httpResponse.getWriter().write(
//                        "{\"error\": \"Profile completion required\", " +
//                                "\"redirectTo\": \"/complete-profile\"}"
//                );
//                return;
//            }
//        }
//
//        chain.doFilter(request, response);
//    }
//
//    private boolean isAllowedPath(String requestURI) {
//        return ALLOWED_PATHS.stream().anyMatch(requestURI::startsWith);
//    }
//}