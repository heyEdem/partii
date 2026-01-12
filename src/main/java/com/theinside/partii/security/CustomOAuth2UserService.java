package com.theinside.partii.security;

import com.theinside.partii.entity.User;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.List;
import java.util.Map;

/**
 * Custom OAuth2 User Service for handling standard OAuth2 providers like GitHub.
 * For OIDC providers (like Google), use CustomOidcUserService instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RestOperations restOperations;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        Object idObj = oAuth2User.getAttribute("id");
        String providerId = idObj != null ? idObj.toString() : null;
        String extractedEmail = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        // fetch GitHub email separately
        if (provider.equals("github") && (extractedEmail == null || extractedEmail.isEmpty())) {
            extractedEmail = fetchGitHubEmail(userRequest);
            log.info("Fetched GitHub email from API: {}", extractedEmail);
        }

        String email = extractedEmail;

        log.info("OAuth2 user authenticated: provider={}, email={}, name={}", provider, email, name);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    existingUser.setEmail(email);
                    existingUser.setLegalName(name);
                    existingUser.setProfilePictureUrl(avatarUrl);
                    log.info("Updating existing user: id={}, email={}", existingUser.getId(), email);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .legalName(name)
                            .profilePictureUrl(avatarUrl)
                            .provider(provider)
                            .providerId(providerId)
                            .build();
                    log.info("Creating new user: email={}, provider={}", email, provider);
                    return userRepository.save(newUser);
                });

        log.info("User saved successfully: id={}, email={}", user.getId(), user.getEmail());

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                oAuth2User.getAttributes(),
                "id"
        );
    }

    /**
     * Fetches the user's email from GitHub's /user/emails API endpoint.
     * GitHub requires a separate API call to get email addresses even with user:email scope.
     */
    private String fetchGitHubEmail(OAuth2UserRequest userRequest) {
        String token = userRequest.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restOperations.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null && !emails.isEmpty()) {
                // Get the primary verified email
                return emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")) &&
                                   Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElseGet(() -> {
                        // If primary verified email is not present, just use the first verified one
                        return emails.stream()
                            .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                            .map(email -> (String) email.get("email"))
                            .findFirst()
                            .orElse(null);
                    });
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub email: {}", e.getMessage());
        }

        return null;
    }
}
