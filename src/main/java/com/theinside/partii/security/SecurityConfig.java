package com.theinside.partii.security;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final KeyManager keyManager;

    public SecurityConfig(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CustomOAuth2UserService customOAuth2UserService,
                                            CustomOidcUserService customOidcUserService,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/partii/api/v1/auth/**",
                                "/.well-known/jwks.json",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/partii/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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


    /**
     * JWT Encoder that builds a fresh NimbusJwtEncoder on every encode() call.
     * This allows key rotation to take effect immediately without restarting.
     */
    @Bean
    JwtEncoder jwtEncoder() {
        return params -> {
            var jwkSource = new ImmutableJWKSet<>(keyManager.getJWKSet());
            return new NimbusJwtEncoder(jwkSource).encode(params);
        };
    }

    /**
     * JWT Decoder that builds a fresh NimbusJwtDecoder on every decode() call.
     * This allows key rotation to take effect immediately without restarting.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            try {
                var decoder = NimbusJwtDecoder.withPublicKey(keyManager.getPublicKey()).build();
                return decoder.decode(token);
            } catch (JwtException e) {
                throw e;
            } catch (Exception e) {
                throw new BadJwtException("Failed to decode JWT: " + e.getMessage(), e);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
