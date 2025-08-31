package com.inventory.dto;

import jakarta.validation.constraints.NotBlank; // Ensures values are neither null nor only whitespace
import lombok.Data;                           // Generates standard JavaBean accessor methods

/**
 * RegisterRequest – DTO representing client-supplied fields for new user registration.
 *
 * Purpose:
 * - This class is used for incoming requests to create new user accounts (POST /api/auth/register).
 * - Ensures a clean decoupling between API payloads and backend persistence (never exposes inner models).
 *
 * Lombok:
 * - @Data automatically provides JavaBean methods (getters/setters/toString/etc.), greatly reducing verbosity.
 *
 * Validation:
 * - @NotBlank is used on required fields, ensuring backend validation BEFORE business logic is called.
 */
@Data
public class RegisterRequest {
    // Username for the new account. Must not be empty. Business logic usually enforces uniqueness.
    @NotBlank
    private String username;

    // Email address for this user account. Must not be empty. Unique check occurs in the service layer.
    @NotBlank
    private String email;

    // Password for this account. Required. Should be encrypted in service layer—never stored or logged in plaintext.
    @NotBlank
    private String password;

    // Optional: First name of the user.
    private String firstName;

    // Optional: Last name of the user.
    private String lastName;

    // The user's role assignment for system authorization. Defaults to "STAFF" for safety—even if attacker tampers with payload.
    // Admin role assignments should only be possible by privileged (admin) users, never during open registration.
    private String role = "STAFF"; // Default value ensures basic access privileges for new users.
}
