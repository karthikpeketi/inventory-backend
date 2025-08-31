package com.inventory.controller;

// These imports bring in DTOs (Data Transfer Objects) used to transfer information between client and backend
// DTOs typically only contain data fields, not business logic

import com.inventory.dto.LoginRequest;
import com.inventory.dto.LoginResponse;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.RefreshTokenRequest;
import com.inventory.dto.ForgotPasswordRequest;
import com.inventory.dto.ForgotPasswordOtpRequest;
import com.inventory.dto.ResetPasswordRequest;
import com.inventory.dto.ResetPasswordWithOtpRequest;
import com.inventory.dto.VerifyPasswordResetOtpRequest;
import com.inventory.service.AuthService; // Service responsible for authentication-related logic
import jakarta.validation.Valid; // Used for input validation

import java.util.Map;

import org.springframework.http.ResponseEntity; // Wrapper for HTTP responses
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // Annotation for handling POST requests
import org.springframework.web.bind.annotation.RequestBody; // Annotation for deserializing JSON request body
import org.springframework.web.bind.annotation.RequestMapping; // Annotation for mapping requests to controllers
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController; // Marks class as a REST controller

/**
 * AuthController handles authentication-related API endpoints.
 *
 * Endpoint base path: /api/auth
 * 
 * Responsibilities:
 * - Manage login (issuance of JWT access tokens)
 * - User registration
 * - Refresh tokens (for issuing new tokens before session expiry)
 *
 * All logic related to authentication is delegated to AuthService.  
 * The controller's job is to translate HTTP requests into service calls and package service results into HTTP responses.
 */
@RestController  // Marks this class as a REST Controller, so Spring can expose its methods as HTTP endpoints returning JSON
@RequestMapping("/api/auth") // All endpoints in this controller will start with /api/auth in the path
public class AuthController {
    
    // The service that handles all authentication business logic
    private final AuthService authService;
    
    // ------
    // Dependency Injection: Spring will "inject" an AuthService instance here when this controller is created.
    // This is a constructor injection, preferred for immutability and required dependency clarity.
    // ------
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Login endpoint
     * POST /api/auth/login
     *
     * Accepts a JSON payload with username and password, authenticates the user,
     * and returns a token with user info if successful.
     * 
     * Summary:
     * - Receives login credentials (username, password)
     * - Delegates authentication to AuthService
     * - Returns a JWT token and user details or an error
     *
     * @param loginRequest DTO deserialized from the request's JSON body
     * @return LoginResponse containing a JWT and user info, wrapped in a 200 OK HTTP response
     */
    @PostMapping("/login") // Listens for POST HTTP requests to /api/auth/login
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest // @Valid: Triggers validation; @RequestBody: Maps JSON to DTO
    ) {
        try {
            // Delegate the authentication process to the AuthService, which checks credentials and generates token
            LoginResponse response = authService.authenticateUser(loginRequest);
            // Return 200 OK HTTP response containing a JSON representation of LoginResponse
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("password") || errorMessage.contains("credentials")) {
                throw new RuntimeException("Please enter the valid password");
            }
            throw new RuntimeException(errorMessage);
        }
    }
    
    /**
     * Register endpoint
     * POST /api/auth/register
     *
     * Allows a new user to register by supplying username, email, and password.
     * The role defaults to "STAFF" unless specified.
     *
     * Summary:
     * - Receives registration details
     * - Delegates user creation and validation to AuthService
     * - Returns confirmation message on success
     *
     * @param registerRequest DTO with registration fields
     * @return 200 OK with a success message string
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Call the self-registration flow (isAdminCreated = false)
            String message = authService.registerUser(registerRequest, false);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("duplicate") || errorMessage.contains("already exists")) {
                return ResponseEntity.badRequest().body(Map.of("message", "User Already Existed", "details", "Username or email is already taken"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMessage));
        }
    }
    
    /**
     * Refresh token endpoint
     * POST /api/auth/refresh-token
     *
     * Used to issue a new access token if the client holds a valid (non-expired) refresh token.
     * This pattern enables longer sessions without constantly prompting for credentials.
     *
     * Summary:
     * - Receives existing refresh token
     * - Delegates validation and new token issuance to AuthService
     * - Returns new JWT token and user info if refresh token is valid
     *
     * @param refreshTokenRequest DTO containing the refresh token
     * @return LoginResponse with a new access token, in 200 OK
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        // Generate a new access token (JWT) for the holder of the refresh token
        LoginResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        // Return the new authentication response
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot password endpoint
     * POST /api/auth/forgot-password
     *
     * Initiates the password reset process by sending a reset link to the user's email.
     * For security reasons, always returns a success message even if the email doesn't exist.
     *
     * Summary:
     * - Receives user's email
     * - Delegates password reset initiation to AuthService
     * - Returns a generic success message for privacy
     *
     * @param forgotPasswordRequest DTO containing the user's email
     * @return 200 OK with a generic success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest
    ) {
        try {
        authService.initiatePasswordReset(forgotPasswordRequest);
        // Always say success for privacy even if user doesn't exist
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link was sent."));
        } catch (RuntimeException e) {
            // Handle specific error cases, e.g., email not found
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    /**
     * Reset password endpoint
     * POST /api/auth/reset-password
     *
     * Completes the password reset process by validating the reset token and updating the password.
     *
     * Summary:
     * - Receives reset token and new password
     * - Delegates token validation and password update to AuthService
     * - Returns confirmation message on success
     *
     * @param req DTO containing the reset token and new password
     * @return 200 OK with a success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req
    ) {

        authService.resetPassword(req);
        return ResponseEntity.ok("Password reset successful");
    }

    /**
     * Verify password reset token
     * GET /api/auth/verify-reset-token
     *
     * Verifies if a password reset token is valid and not expired.
     * This endpoint is used by the frontend to validate tokens before showing the reset form.
     *
     * @param token The reset token to verify
     * @return 200 OK if token is valid, 400 Bad Request if invalid/expired
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<Map<String, Object>> verifyResetToken(
            @RequestParam("token") String token
    ) {
        try {
            boolean isValid = authService.verifyResetToken(token);
            if (isValid) {
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Token is valid"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Invalid or expired reset token"
                ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Initiate password reset with OTP
     * POST /api/auth/forgot-password-otp
     *
     * Initiates the password reset process by sending an OTP to the user's email.
     * For security reasons, always returns a success message even if the email doesn't exist.
     *
     * @param request DTO containing the user's email
     * @return 200 OK with a generic success message
     */
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<Map<String, String>> forgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpRequest request
    ) {
        authService.initiatePasswordResetWithOtp(request);
        // Always say success for privacy even if user doesn't exist
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, an OTP was sent."));
    }

    /**
     * Verify password reset OTP
     * POST /api/auth/verify-password-reset-otp
     *
     * Verifies the OTP sent for password reset.
     *
     * @param request DTO containing email and OTP
     * @return 200 OK with verification result
     */
    @PostMapping("/verify-password-reset-otp")
    public ResponseEntity<Map<String, Object>> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request
    ) {
        boolean verified = authService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(Map.of(
            "verified", verified,
            "message", verified ? "OTP verified successfully" : "Invalid or expired OTP"
        ));
    }

    /**
     * Reset password with OTP
     * POST /api/auth/reset-password-otp
     *
     * Completes the password reset process using verified OTP.
     *
     * @param request DTO containing email, OTP, and new password
     * @return 200 OK with success message
     */
    @PostMapping("/reset-password-otp")
    public ResponseEntity<Map<String, String>> resetPasswordWithOtp(
            @Valid @RequestBody ResetPasswordWithOtpRequest request
    ) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }
    
    /**
     * Logout endpoint
     * POST /api/auth/logout
     *
     * Logs out the current user. Since JWT is stateless, this primarily serves as a notification
     * to the backend that the user has logged out. The frontend should clear the token. This method
     * can be extended for refresh token invalidation or other session management in the future.
     *
     * Summary:
     * - Receives logout request
     * - Delegates to AuthService for potential future session management
     * - Returns a success message
     *
     * @return 200 OK with a success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // Delegate to AuthService for potential future enhancements like refresh token invalidation
        String message = authService.logout();
        return ResponseEntity.ok(Map.of("message", message));
    }
}
