package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LoginRequest – DTO for user login via username or email and password.
 *
 * Purpose:
 * - Used for POST requests to the authentication endpoint.
 * - Accepts either username or email along with password.
 *
 * Lombok:
 * - @Data provides getters, setters, equals, hashCode, and toString methods.
 *
 * Validation:
 * - @NotBlank ensures fields are not null or empty.
 */
@Data
public class LoginRequest {
    
    // Username or Email supplied by client for authentication
    @NotBlank(message = "Username or Email cannot be empty")
    private String usernameOrEmail;

    // Password supplied by client
    @NotBlank(message = "Password cannot be empty")
    private String password;
}
