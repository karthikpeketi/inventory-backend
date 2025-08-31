package com.inventory.security.jwt;

import com.inventory.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AuthTokenFilter is a custom security filter for validating JWTs in Spring Security pipelines.
 * 
 * <p>
 * SUMMARY: 
 * This filter intercepts every HTTP request only once (extending OncePerRequestFilter).
 * It attempts to extract a JWT token from the request header, validates it,
 * then, if valid, loads the associated user details and sets up Spring Security context for authorization.
 * This is a common pattern for enabling stateless, token-based authentication in RESTful APIs.
 * </p>
 */
@Component // Mark this class as a Spring-managed bean so it can be used in dependency injection
public class AuthTokenFilter extends OncePerRequestFilter {

    // Logger object for writing logs specific to this filter class
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    // Dependencies required by the filter

    // Utility class for JWT operations: validating, parsing, etc.
    private final JwtUtils jwtUtils;
    // Custom implementation to load user-specific data (used for authentication)
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Constructor for AuthTokenFilter.
     * Uses constructor-based dependency injection for JwtUtils and UserDetailsServiceImpl, 
     * promoting immutability and testability.
     */
    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core method executed on every HTTP request.
     * <p>
     * Attempts to extract JWT from the Authorization header, validate the token, 
     * then (if valid) loads the user details and sets the SecurityContext for the request, 
     * establishing the user's identity and authorities (roles/permissions) for downstream security checks.
     * <p>
     * Any exception during this process is caught, logged, and not propagated, 
     * allowing the request to continue (typically resulting in an unauthenticated or forbidden response if JWT is invalid/missing).
     *
     * @param request      The incoming HTTP request to be filtered
     * @param response     The HTTP response object
     * @param filterChain  The filter chain to pass processing to the next filter
     * @throws ServletException if the filter process fails
     * @throws IOException      if I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Use try-catch to avoid filter-chain interruption in case of token errors or user lookup failures
        try {
            // Extract a JWT token from the request (usually in the "Authorization" header as "Bearer <token>")
            String jwt = parseJwt(request);

            // Check if a JWT was found and is valid
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Decode username from the JWT's payload
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // Load user's details (including authorities/roles) using the username
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create an authentication token for Spring Security context
                //  - userDetails: principal data (username, password, roles)
                //  - null: credentials (passwords) aren't needed at this stage
                //  - userDetails.getAuthorities(): user's authorities (permissions/roles)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Attach additional request-specific details (such as IP address, session) to the authentication token
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authenticated user context so downstream security operations know the user is authenticated
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log errors related to JWT parsing or user lookup to ease debugging
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Proceed with the rest of the filter chain (continue processing the request)
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to extract the JWT token from the "Authorization" HTTP header.
     * <p>
     * - Looks for the header's presence and checks that it starts with the "Bearer " scheme (case sensitive).
     * - If found, removes the "Bearer " prefix and returns the token string.
     * - Returns null if no token is found or the header is malformed.
     *
     * @param request The incoming HTTP request
     * @return JWT token as a String if found; otherwise null
     */
    private String parseJwt(HttpServletRequest request) {
        // Get the value of "Authorization" header from the request
        String headerAuth = request.getHeader("Authorization");

        // Ensure header is not empty and uses expected Bearer scheme
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // Return only the token part after the "Bearer " prefix
            return headerAuth.substring(7);
        }
        // Return null if not properly present
        return null;
    }
}