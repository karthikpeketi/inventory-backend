package com.inventory;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class LocalInventoryManagerApplication {

    private static final Logger logger = LoggerFactory.getLogger(LocalInventoryManagerApplication.class);

    public static void main(String[] args) {
        // Determine the active profile
        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile == null || profile.trim().isEmpty()) {
            profile = "development"; // Default to development
        }

        // Construct the .env filename based on the profile
        String envFileName = ".env." + profile;
        logger.info("Loading environment variables from: {}", envFileName);

        // Load the appropriate .env file
        Dotenv dotenv = Dotenv.configure()
                .filename(envFileName)
                .ignoreIfMissing()
                .load();

        // Set system properties from .env file
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        logger.info("SPRING_PROFILES_ACTIVE: {}", dotenv.get("SPRING_PROFILES_ACTIVE"));


        SpringApplication.run(LocalInventoryManagerApplication.class, args);
    }
}
