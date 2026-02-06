package com.theinside.partii.security;

import com.theinside.partii.entity.User;
import com.theinside.partii.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Handles successful OAuth2 authentication by issuing JWT tokens and redirecting
 * to the frontend with tokens as query parameters.
 * 
 * This is the standard SPA pattern for OAuth2 flows where the backend cannot
 * return JSON directly during the redirect-based OAuth2 flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    @Value("${partii.oauth2.redirect-uri:http://localhost:3000/oauth-callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            log.error("Unexpected authentication type: {}", authentication.getClass());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        OAuth2User oauth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        
        // Extract email from OAuth2 user attributes
        String email = extractEmail(oauth2User, provider);
        
        if (email == null) {
            log.error("Could not extract email from OAuth2 provider: {}", provider);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth provider");
            return;
        }

        log.info("OAuth2 authentication successful for provider: {}, email: {}", provider, email);

        // Load user from database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found after OAuth2 authentication: {}", email);
                    return new RuntimeException("User not found");
                });

        // Issue JWT tokens
        var authToken = tokenManager.issueToken(user);

        // Build redirect URL with tokens
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", authToken.getAccessToken())
                .queryParam("refresh_token", authToken.getRefreshToken())
                .queryParam("expires_at", DateTimeFormatter.ISO_INSTANT.format(
                        authToken.getAccessTokenExpiresAt().atOffset(ZoneOffset.UTC)))
                .queryParam("user_id", user.getId())
                .queryParam("email", encodeURIComponent(user.getEmail()))
                .queryParam("display_name", encodeURIComponent(user.getDisplayName()))
                .queryParam("verified", user.isVerified())
                .build().toUriString();

        log.info("Redirecting to frontend after OAuth2 login for user: {}", user.getId());
        response.sendRedirect(targetUrl);
    }

    /**
     * Extract email from OAuth2 user attributes based on provider.
     */
    private String extractEmail(OAuth2User oauth2User, String provider) {
        String email = oauth2User.getAttribute("email");
        
        if (email == null && "github".equals(provider)) {
            // GitHub may not always return email in user info, might need to fetch from /user/emails
            // For now, try to get from attributes
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> emails = oauth2User.getAttribute("emails");
            if (emails != null && !emails.isEmpty()) {
                email = (String) emails.get(0).get("email");
            }
        }
        
        return email;
    }

    /**
     * URL-encode a string for safe use in query parameters.
     */
    private String encodeURIComponent(String value) {
        if (value == null) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
