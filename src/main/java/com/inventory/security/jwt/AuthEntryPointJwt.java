package com.inventory.security.jwt;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * AuthEntryPointJwt – Custom authentication entry point for handling unauthorized API access.
 *
 * Background:
 * - In a typical stateless JWT-secured API, when a user presents invalid or missing credentials,
 *   Spring Security intercepts the request and invokes AuthenticationEntryPoint.
 * - This class configures a custom response (usually HTTP 401 with error JSON/message)
 *   instead of a generic browser redirect or silent fail.
 *
 * Key Concepts:
 * - Implements AuthenticationEntryPoint (Spring Security's pluggable contract for error handling).
 * - Marked as @Component for auto-registration in Spring's application context.
 * - Logs the authentication error (for audits/ops) and sends a clear 401 Unauthorized HTTP code to the client.
 * 
 * Security:
 * - Do not leak sensitive details in error messages; only communicate the access restriction.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    // Logger for diagnostics and audit trails
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    /**
     * Called by Spring Security anytime an unauthenticated user requests a protected resource.
     *
     * @param request         Incoming HTTP request
     * @param response        HTTP response object to configure for the error
     * @param authException   The reason authentication failed (invalid/missing JWT, etc)
     * @throws IOException    Forwarded from HttpServletResponse
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Log the full error (without exposing secret details to clients)
        logger.error("Unauthorized error: {}", authException.getMessage());

        // Send HTTP 401 status with a fixed "Error: Unauthorized" message
        // This tells the client (browser, mobile app, REST consumer) they must supply login.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
    }
}
