
package com.inventory.security;

import com.inventory.security.jwt.AuthEntryPointJwt;
import com.inventory.security.jwt.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SecurityConfig
 *
 * <p>
 * This Java configuration class sets up most aspects of Spring Security for the API.
 * It covers: authentication, authorization rules, JWT filtering, session management, password encoding, and CORS setup.
 * </p>
 */
@Configuration // Indicates this is a configuration class for the Spring context
@EnableWebSecurity // Enables Spring Security support in the application
@EnableMethodSecurity // Allows method-level security annotations (e.g., @PreAuthorize)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    // Dependencies injected by Spring using constructor-based injection

    // Custom handler for unauthorized access (commonly returns 401 errors for unauthenticated users)
    private final AuthEntryPointJwt unauthorizedHandler;
    // Filter to extract and validate JWT tokens from requests
    private final AuthTokenFilter authTokenFilter;
    
    // Constructor for initializing dependencies used by this configuration
    public SecurityConfig(AuthEntryPointJwt unauthorizedHandler, AuthTokenFilter authTokenFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.authTokenFilter = authTokenFilter;
    }

    /**
     * Defines the application-wide Spring Security filter chain and rules.
     * <p>
     * Key configuration points:
     * - CORS: Handled by CorsFilter (disabled here to avoid conflicts)
     * - CSRF: Disabled (not needed for stateless APIs)
     * - Exception handling: Customizes unauthorized response logic
     * - Session management: Enforces stateless (no HTTP session; via tokens only)
     * - Authorization: Configures public vs. authenticated-only endpoints
     * - Adds the JWT authentication filter before the standard login filter
     *
     * @param http The HttpSecurity configurer for building security settings
     * @return Configured SecurityFilterChain for Spring Security to use
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security filter chain - CORS handled by CorsFilter");
        
        http
            // Disable Spring Security CORS to avoid conflicts with our CorsFilter
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**", "/api/auth/reset-password/**", "/api/auth/verify-reset-token", "/api/users/verify-activation-token", "/api/users/verify-activation-token/**", "/api/users/activate-account", "/api/users/check-username").permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**").permitAll()
                .anyRequest().authenticated()
            )

            // Keep JWT filter
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS configuration removed - now handled by CorsFilter to avoid conflicts

    /**
     * Provides the authentication manager used by Spring Security for login and authentication logic.
     * The AuthenticationManager is central to all authorization requests (logins, JWT verifications, etc).
     *
     * @param authConfig Spring's AuthenticationConfiguration (auto-wired by the context)
     * @return An AuthenticationManager instance
     * @throws Exception if there is a problem creating the manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configures the password hasher for secure user password storage and checks.
     * Uses the BCrypt hashing algorithm (an industry best-practice for storing passwords securely).
     * @return PasswordEncoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
