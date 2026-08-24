package com.agrawalpulse.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Applies to every profile identically - the only thing that differs per profile is *how*
// the JwtDecoder validates tokens (see security.local.LocalJwtConfig for "local", and
// spring.security.oauth2.resourceserver.jwt.jwk-set-uri in application-{dev,staging,prod}.yml
// for Cognito). This keeps tenant/role enforcement code identical across environments.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${agrawalpulse.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Only reachable at all when the local profile is active - LocalTokenController
                        // itself is @Profile("local"), so this route 404s everywhere else.
                        .requestMatchers("/api/v1/local-auth/**").permitAll()
                        // Swagger UI itself needs no auth to load; the APIs it calls still enforce JWT
                        // normally via the "Authorize" button. Disabled outright in prod (see
                        // application-prod.yml) so this permitAll has no effect there.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Chapter/branch directory data (name/city/state - no personal data) needs to be
                        // browsable BEFORE a user has a token: the login screen shows a branch picker so
                        // people can select which branch to authenticate against, and local-auth/token
                        // requires a chapterId up front - there's no way to have a token before this call.
                        // Only GET is opened up; POST /api/v1/chapters still requires authentication (on
                        // top of its own @PreAuthorize("hasRole('NATIONAL_ADMIN')") in ChapterController).
                        // Only user-service actually maps /api/v1/chapters, so this is a no-op filter rule
                        // in every other service (same pattern as /api/v1/local-auth/** below).
                        .requestMatchers(HttpMethod.GET, "/api/v1/chapters/**").permitAll()
                        // Public sign-up: a brand-new account has no JWT to authenticate with yet.
                        // registerUser (see UserController/UserService) always assigns the USER role
                        // regardless of the request body, so opening this up cannot mint an admin.
                        // Only user-service maps this path, so it's a no-op filter rule elsewhere
                        // (same pattern as /api/v1/local-auth/** and the chapters GET rule above).
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRolesConverter());
        return converter;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
