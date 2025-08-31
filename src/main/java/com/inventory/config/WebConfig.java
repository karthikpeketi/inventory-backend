package com.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig: Spring Web MVC Configuration for the Inventory Management Backend.
 *
 * This class is responsible for configuring web-related settings for the entire Spring Boot application.
 * The most notable responsibility here is customizing the CORS (Cross-Origin Resource Sharing) policy.
 * 
 * CORS is a fundamental concept for controlling how your backend API can be accessed by frontend applications 
 * hosted on different origins (domains, ports, or protocols). Without explicit CORS configuration, browsers 
 * will block requests from a different origin as a security measure.
 */
@Configuration  // Marks this class as a source of bean definitions and configuration for Spring's context.
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Configures CORS settings for the application's REST endpoints.
     *
     * This method is automatically called by Spring at startup to register global CORS mappings.
     * By customizing this, you define which external domains ("origins") may call your backend API,
     * which HTTP methods and headers are allowed, and whether credentials (cookies, HTTP auth) are supported.
     *
     * Why is this important?
     * - If your frontend (e.g., React app at localhost:3000) runs in the browser and tries to fetch data from
     *   the backend (e.g., Spring Boot app at localhost:8080), the browser enforces CORS policies, and backend needs to permit that.
     *
     * @param registry CorsRegistry object to which your CORS rules are added.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
