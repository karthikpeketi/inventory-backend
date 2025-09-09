package com.inventory.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Global CORS filter to ensure CORS headers are always present.
 * This acts as a bulletproof CORS solution for all requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    @Value("${cors.allowed-origins:https://inventory-frontend-kappa-ten.vercel.app,http://localhost:3000,http://127.0.0.1:3000}")
    private String corsAllowedOrigins;
    
    private List<String> allowedOriginsList;

    @Override
    public void init(FilterConfig filterConfig) {
        // Parse allowed origins once during initialization
        allowedOriginsList = Arrays.asList(corsAllowedOrigins.split(","));
        logger.info("CORS Filter initialized with allowed origins: {}", allowedOriginsList);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        
        logger.debug("Processing request: {} {} from origin: {}", method, request.getRequestURI(), origin);

        // Handle CORS based on origin
        if (origin != null) {
            // Check if the origin is in our allowed origins list (exact match)
            if (allowedOriginsList.contains(origin.trim())) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                logger.debug("Allowed origin: {}", origin);
            } else {
                logger.warn("Origin not allowed: {}. Allowed origins: {}", origin, allowedOriginsList);
                // For security, don't set CORS headers for disallowed origins
            }
        } else {
            // For requests without Origin header (like direct API calls, mobile apps, etc.)
            // We'll allow them but log for monitoring
            logger.debug("Request without Origin header - allowing");
        }

        // Set all CORS headers
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        response.setHeader("Access-Control-Allow-Headers", 
            "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
        
        // Add additional headers to prevent caching issues
        response.setHeader("Vary", "Origin");

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("Handling preflight OPTIONS request for origin: {}", origin);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}