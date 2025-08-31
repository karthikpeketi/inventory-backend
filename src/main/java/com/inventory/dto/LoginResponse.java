package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginResponse – DTO representing successful authentication details.
 *
 * Purpose:
 * - Sent to the client after successful user authentication (upon login or token refresh).
 * - Provides both the access token (for client to use in future secure requests)
 *   and several user details (for UI and context).
 *
 * Lombok:
 * - @Data           : Generates getters, setters, equals, hashCode, and toString methods.
 * - @Builder        : Enables fluent object construction pattern for cleaner instantiation.
 * - @NoArgsConstructor, @AllArgsConstructor: Ensures flexible object creation by frameworks, 
 *                    mappers, or manual instantiation.
 *
 * API Security Note:
 * - The token should be handled securely on the client (stored in memory, secure local storage, 
 *   or httpOnly cookie).
 * - Exposing user id/email is often useful for client-side displays and role-based controls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /**
     * The JWT token string, to be used as a Bearer token in Authorization headers.
     * Format: typically "xxxxxx.yyyyyyy.zzzzzz" (header.payload.signature)
     */
    private String token;

    /**
     * The unique user ID for the authenticated client.
     * Used for tracking, auditing, and database references.
     */
    private Integer id;

    /**
     * The username of the authenticated user.
     * Often displayed in UI elements (e.g., "Welcome, {username}").
     */
    private String username;

    /**
     * The user's email address.
     * May be used for notifications or as an alternative identifier.
     */
    private String email;

    /**
     * The user's assigned application role (e.g., "ADMIN", "STAFF", "USER").
     * Used for role-based access control on the client side.
     */
    private String role;

    private String firstName;

    private String lastName;
}
