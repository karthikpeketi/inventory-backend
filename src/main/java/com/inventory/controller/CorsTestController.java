package com.inventory.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify CORS functionality
 * Provides debugging endpoints to check CORS configuration
 */
@RestController
@RequestMapping("/api/cors")
public class CorsTestController {

    private static final Logger logger = LoggerFactory.getLogger(CorsTestController.class);

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Simple GET endpoint to test CORS
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testCors(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String userAgent = request.getHeader("User-Agent");
        
        logger.info("CORS test request from origin: {}", origin);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS is working!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("origin", origin);
        response.put("allowedOrigins", corsAllowedOrigins);
        response.put("method", request.getMethod());
        response.put("requestUri", request.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint to test CORS with preflight
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testCorsPost(@RequestBody(required = false) Map<String, Object> payload, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        
        logger.info("CORS POST test request from origin: {}", origin);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS POST is working!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("origin", origin);
        response.put("allowedOrigins", corsAllowedOrigins);
        response.put("receivedPayload", payload);
        response.put("method", request.getMethod());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to show all request headers
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugCors(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        
        logger.info("CORS debug request from origin: {}", origin);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS Debug Information");
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Add all headers
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            headers.put(headerName, request.getHeader(headerName)));
        
        response.put("headers", headers);
        response.put("method", request.getMethod());
        response.put("requestUri", request.getRequestURI());
        response.put("allowedOrigins", corsAllowedOrigins);
        response.put("remoteAddr", request.getRemoteAddr());
        response.put("serverName", request.getServerName());
        response.put("serverPort", request.getServerPort());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "inventory-backend");
        return ResponseEntity.ok(response);
    }
}