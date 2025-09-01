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

}
