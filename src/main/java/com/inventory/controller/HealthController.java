package com.inventory.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller to verify the application is running
 * and CORS is properly configured.
 */
@RestController
@RequestMapping("/api")
// @CrossOrigin removed - CORS handled by CorsFilter to avoid conflicts
public class HealthController {

    @Value("${cors.allowed-origins:https://inventory-frontend-kappa-ten.vercel.app}")
    private String corsAllowedOrigins;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Backend is running");
        response.put("corsAllowedOrigins", corsAllowedOrigins);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // OPTIONS method removed - handled by CorsFilter
}