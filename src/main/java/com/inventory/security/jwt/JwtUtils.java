package com.inventory.security.jwt;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


import com.inventory.security.service.UserDetailsImpl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import javax.crypto.SecretKey;

/**
 * JwtUtils
 *
 * Utility class for handling all operations related to JWT (JSON Web Token) in the application.
 * It covers token creation, parsing, and validation using a symmetric secret key with the jjwt library.
 *
 * <p>
 * This class promotes security mechanisms by ensuring only authenticated and authorized
 * requests can access protected resources, following a stateless authentication pattern.
 * </p>
 */
@Component // Marks the class as a Spring component (makes it available for dependency injection)
public class JwtUtils {
    // SLF4J Logger for this class, used for error/info/debug logging
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // ================================
    // Configuration properties injected from application properties
    // ================================

    // Secret used for signing and verifying JWTs (should be long, random, and kept private)
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Token validity period (in milliseconds): how long the token is valid after creation
    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;




    // ================================
    // Internal utility: obtain the SecretKey instance for signing/verifying JWTs
    // ================================

    /**
     * Returns a SecretKey instance derived from the configured JWT secret.
     * This uses jjwt's recommended key management (`Keys.hmacShaKeyFor`) for HMAC-based algorithms.
     * <p>
     * By keeping the key creation in one place, it avoids accidental key inconsistencies
     * and ensures the right encoding for token operations.
     */
    private SecretKey getSigningKey() {
        // Convert string secret to byte array to generate a cryptographic secret key
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ================================
    // Generate a JWT token for an authenticated user
    // ================================

    /**
     * Generates a JWT token for the given authenticated user.
     * - Includes the user's username as the "subject" claim.
     * - Sets creation (iat) and expiration (exp) timestamps.
     * - Signs the JWT with the configured secret and algorithm.
     *
     * @param authentication Spring Security Authentication object (represents the user)
     * @return Signed JWT token as a String
     */
    public String generateJwtToken(Authentication authentication) {
        // Extract the authenticated user's principal details
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        // Build the JWT with subject, issue and expiration dates, sign it, and return the compact (string) form
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())    // Main identifier: set as JWT subject claim
                .setIssuedAt(new Date())                   // Token creation time
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Set expiration from now
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)              // Sign with HMAC SHA512 and secret key
                .compact();                                // Return as a String
    }

    // ================================
    // Extract the username (subject) from a JWT token string
    // ================================

    /**
     * Parses the JWT token and extracts the username (the "subject" claim).
     *
     * @param token The JWT token String
     * @return Username found in the token's subject claim
     * @throws JwtException if the token is invalid in any way (signature, expired, etc.)
     */
    public String getUserNameFromJwtToken(String token) {
        // Parse the token, supplying the signing key, and return the subject (username)
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())            // Secret key needed for verifying token signature
                .build()
                .parseClaimsJws(token)                     // This both verifies signature and returns claims
                .getBody()
                .getSubject();                             // Extract and return subject claim (the username)
    }

    // ================================
    // Validate a JWT token: checks signature, expiry, and format
    // ================================

    /**
     * Validates the JWT token string. 
     * - Verifies the token's signature matches the secret key.
     * - Checks expiration and supported structure.
     * - Handles all expected exceptions by logging a specific error.
     *
     * @param authToken The JWT token to validate
     * @return true if valid; false otherwise (expired, malformed, unsupported, empty, bad signature, etc.)
     */
    public boolean validateJwtToken(String authToken) {
        try {
            // Parse the JWT; if parsing is successful, token is valid
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            // Signature was invalid (potential tampering or bad secret)
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            // Token structure does not follow JWT format
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // "exp" claim shows token is no longer valid
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // Structure/algorithm/contents not supported
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // Token string was null or empty
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        // Return false in any failure case
        return false;
    }


}