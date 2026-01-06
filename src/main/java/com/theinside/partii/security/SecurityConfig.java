package com.theinside.partii.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CustomOAuth2UserService customOAuth2UserService,
                                            CustomOidcUserService customOidcUserService) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/partii/api/v1/auth/home", "/partii/api/v1/auth/logout", "/partii/api/v1/auth/logout-success").permitAll()
                        .anyRequest().authenticated())
                .formLogin(withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/partii/api/v1/auth/user", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)))
                .logout(logout -> logout
                        .logoutUrl("/partii/api/v1/auth/logout")
                        .logoutSuccessUrl("/partii/api/v1/auth/logout-success")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }

    /**
     * Configure RestTemplate with longer timeouts for OAuth2 operations.
     * This is critical for JWT validation which requires fetching JWK keys from providers.
     */
    @Bean
    public RestOperations restOperations() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return new RestTemplate(requestFactory);
    }

    /**
     * JWT Decoder factory for validating ID tokens from OAuth2 providers.
     * Uses custom RestTemplate with extended timeouts.
     */
    @Bean
    public JwtDecoderFactory<ClientRegistration> jwtDecoderFactory(RestOperations restOperations) {
        return clientRegistration -> {
            String jwkSetUri = clientRegistration.getProviderDetails()
                    .getJwkSetUri();

            if (jwkSetUri != null) {
                return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .restOperations(restOperations)
                        .build();
            }

            throw new IllegalArgumentException(
                "ClientRegistration must have a JWK Set URI configured");
        };
    }
}
