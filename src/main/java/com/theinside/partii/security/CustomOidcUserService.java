package com.theinside.partii.security;

import com.theinside.partii.entity.User;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Custom OIDC User Service for handling OpenID Connect providers like Google.
 * OIDC is an identity layer on top of OAuth2 that provides ID tokens with user claims.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        String providerId = oidcUser.getAttribute("sub");
        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");
        String avatarUrl = oidcUser.getAttribute("picture");

        log.info("OIDC user authenticated: provider={}, email={}, name={}, sub={}",
                 provider, email, name, providerId);

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

        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
}
