package com.theinside.partii.security;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.theinside.partii.entity.User;
import com.theinside.partii.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
                                            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/partii/api/v1/auth/**",
                                "/error"
                        ).permitAll()
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
                        .deleteCookies("JSESSIONID"))
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

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(UserRepository userRepository) {
        return jwt -> {
            String email = jwt.getSubject();

            User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

            SecurityUser securityUser = new SecurityUser(user);

            return new UsernamePasswordAuthenticationToken(
                    user,
                    null
            );
        };

    }
    @Bean
    JwtEncoder jwtEncoder(){
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(keyManager.getJWKSet());
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(keyManager.getPublicKey()).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
