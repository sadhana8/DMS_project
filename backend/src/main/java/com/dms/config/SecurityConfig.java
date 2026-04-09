package com.dms.config;

import com.dms.security.JwtAuthenticationFilter;
import com.dms.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration for the DocVault application.
 *
 * <h2>Security model</h2>
 * <ul>
 * <li><b>Stateless sessions</b> – no HTTP session is ever created; every
 * request must carry a valid JWT in the {@code Authorization: Bearer}
 * header.</li>
 * <li><b>JWT filter</b> – {@link JwtAuthenticationFilter} runs before
 * {@link UsernamePasswordAuthenticationFilter} on every request and populates
 * the {@link org.springframework.security.core.context.SecurityContext}.</li>
 * <li><b>RBAC</b> – coarse-grained URL rules here plus fine-grained
 * {@code @PreAuthorize} annotations on controller methods.</li>
 * <li><b>CORS</b> – only the configured frontend origin is allowed.</li>
 * <li><b>CSRF</b> – disabled because JWT-based APIs are not vulnerable to
 * CSRF.</li>
 * </ul>
 *
 * <h2>Public endpoints (no JWT required)</h2>
 * <ul>
 * <li>{@code POST /auth/login}</li>
 * <li>{@code POST /auth/register}</li>
 * <li>{@code POST /auth/forgot-password}</li>
 * <li>{@code POST /auth/reset-password}</li>
 * <li>{@code POST /auth/verify-email}</li>
 * <li>{@code POST /auth/refresh-token}</li>
 * <li>{@code OPTIONS /**} – preflight requests</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see JwtAuthenticationFilter
 * @see CustomUserDetailsService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT authentication filter injected and added to the filter chain.
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Custom
     * {@link org.springframework.security.core.userdetails.UserDetailsService}
     * that loads users by e-mail from the database.
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * The URL of the React frontend, read from {@code app.frontend.url}. Used
     * to build the CORS allow-list.
     */
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Defines the main HTTP security filter chain.
     *
     * <p>
     * Key decisions:
     * <ul>
     * <li>CSRF protection is disabled – safe for stateless JWT APIs.</li>
     * <li>Session creation is set to {@code STATELESS} – no {@code JSESSIONID}
     * cookies.</li>
     * <li>Admin-only paths ({@code /admin/**}) require {@code ROLE_ADMIN}.</li>
     * <li>User-management paths ({@code /users/**}) require at least
     * {@code ROLE_MANAGER}.</li>
     * <li>Everything else requires the user to be authenticated (valid
     * JWT).</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring
     * @return the built and immutable {@link SecurityFilterChain}
     * @throws Exception if the configuration cannot be applied
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/login", "/auth/register",
                        "/auth/forgot-password", "/auth/reset-password",
                        "/auth/verify-email", "/auth/refresh-token"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Builds the CORS policy applied to all API endpoints.
     *
     * <p>
     * Allows the configured frontend origin plus {@code localhost:3000} for
     * local development, all standard HTTP methods, and any request headers.
     * Credentials (cookies / Authorization headers) are explicitly permitted.
     * The preflight result is cached for one hour to reduce OPTIONS
     * round-trips.
     *
     * @return a {@link CorsConfigurationSource} registered for all paths
     * ({@code /**})
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl, "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Configures a {@link DaoAuthenticationProvider} that delegates user lookup
     * to {@link CustomUserDetailsService} and password verification to BCrypt.
     *
     * @return the configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean so it can be
     * injected into {@link com.dms.service.impl.AuthServiceImpl} for explicit
     * credential validation during login.
     *
     * @param config the auto-configured {@link AuthenticationConfiguration}
     * @return the application-wide {@link AuthenticationManager}
     * @throws Exception if the manager cannot be retrieved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a BCrypt {@link PasswordEncoder} with the default strength
     * factor (10).
     *
     * <p>
     * BCrypt automatically salts each hash, making rainbow-table attacks
     * impractical.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
