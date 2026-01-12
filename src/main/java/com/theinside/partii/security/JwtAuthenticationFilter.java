package com.theinside.partii.security;

import com.theinside.partii.entity.User;
import com.theinside.partii.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that intercepts incoming requests and validates JWT bearer tokens.
 *
 * Flow:
 * 1. Extract token from Authorization header (Bearer scheme)
 * 2. Decode and validate JWT signature and expiry
 * 3. Load user from database using token subject (email)
 * 4. Set authentication in SecurityContext for downstream access
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Skip if no Authorization header or not Bearer scheme
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if already authenticated (e.g., via OAuth2 session)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Jwt jwt = jwtDecoder.decode(token);

            String email = jwt.getSubject();

            userRepository.findByEmail(email).ifPresent(user -> {
                if (user.isEnabled()) {
                    SecurityUser securityUser = new SecurityUser(user);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    securityUser,
                                    null,
                                    securityUser.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authenticated user: {}", email);
                } else {
                    logger.warn("Attempted access by disabled user: {}", email);
                }
            });

        } catch (JwtException e) {
            logger.debug("JWT validation failed: {}", e.getMessage());
            // Don't set authentication - let Spring Security handle the 401
        }

        filterChain.doFilter(request, response);
    }
}
