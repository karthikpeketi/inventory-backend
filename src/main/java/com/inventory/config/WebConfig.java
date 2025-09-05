package com.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig: Spring Web MVC Configuration for the Inventory Management Backend.
 *
 * This class is responsible for configuring web-related settings for the entire Spring Boot application.
 * 
 * CORS is a fundamental concept for controlling how your backend API can be accessed by frontend applications 
 * hosted on different origins (domains, ports, or protocols). Without explicit CORS configuration, browsers 
 * will block requests from a different origin as a security measure.
 * 
 * Note: CORS configuration is handled by Spring Security's SecurityConfig to ensure proper integration
 * with authentication and authorization mechanisms.
 */
@Configuration  // Marks this class as a source of bean definitions and configuration for Spring's context.
public class WebConfig implements WebMvcConfigurer {

    // CORS configuration is handled by Spring Security's SecurityConfig
    // This ensures CORS works properly with JWT authentication
}
