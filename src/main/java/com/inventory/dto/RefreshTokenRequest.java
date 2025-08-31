package com.inventory.dto;

import lombok.Data; // Generates getters, setters, equals, hashCode, and toString automatically

/**
 * RefreshTokenRequest – DTO representing a client's request to refresh its authentication token.
 *
 * Purpose:
 * - Used in authentication flows ("refresh token grant") where a client, after authentication,
 *   wants to obtain a new (short-lived) access token without prompting the user for credentials again.
 * - Helps enable persistent user sessions without compromising security.
 *
 * Security:
 * - The refresh token should be a long, random value. It must be protected by HTTPS and never logged in plaintext.
 * - The backend validates this token and, if valid and not expired, issues a new access token.
 *
 * Lombok:
 * - @Data provides standard JavaBean conventions, making API parsing/serialization simple.
 */
@Data
public class RefreshTokenRequest {
    // The client's previously-issued refresh token (should only be handled over secure channels!)
    private String refreshToken;
}
