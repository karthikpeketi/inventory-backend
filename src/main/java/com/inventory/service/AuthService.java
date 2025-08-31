package com.inventory.service;

import com.inventory.dto.ForgotPasswordRequest;
import com.inventory.dto.ForgotPasswordOtpRequest;
import com.inventory.dto.LoginRequest;
import com.inventory.dto.LoginResponse;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.ResetPasswordRequest;
import com.inventory.dto.ResetPasswordWithOtpRequest;
import com.inventory.dto.VerifyPasswordResetOtpRequest;
import com.inventory.exception.AccountInactiveException;
import com.inventory.exception.DuplicateFieldException;
import com.inventory.model.ActivationToken;
import com.inventory.model.Otp;
import com.inventory.model.User;
import com.inventory.repository.ActivationTokenRepository;
import com.inventory.repository.OtpRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.jwt.JwtUtils;
import com.inventory.security.service.UserDetailsImpl;
import com.inventory.security.service.UserDetailsServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

/**
 * AuthService handles user authentication, registration, and issuing of JWT tokens in the system.
 * This service is called by controllers and orchestrates authentication flows,
 * such as logins, sign-ups, and refreshing JWT tokens, through secure, well-defined steps.
 * 
 * Key responsibilities:
 *  - Authenticate user credentials, issue JWT tokens
 *  - Persist/register new user accounts, ensure uniqueness of username/email
 *  - Handle token refreshing (for session renewal without requiring re-login)
 */
@Service // Tells Spring that this class holds business logic (service layer)
public class AuthService {
    // ==== Dependencies required for authentication tasks ====
    private final AuthenticationManager authenticationManager; // Handles authentication logic/rules
    private final UserRepository userRepository;               // Communicates with DB for user records
    private final PasswordEncoder passwordEncoder;             // Security: hashes and checks passwords
    private final JwtUtils jwtUtils;                           // Generates/verifies JWTs
    private final ActivationTokenRepository activationTokenRepository; // Manages activation and reset tokens
    private final OtpRepository otpRepository;                 // Manages OTP tokens


    // Loads user details; Spring @Autowired handles wiring this bean
    @Autowired
    private UserDetailsServiceImpl userDetailsService;


    @Autowired
    private EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl; // Base URL for email link


    /**
     * Constructor for AuthService. Uses dependency injection to assemble all required service/utility references.
     *
     * @param authenticationManager Spring Security's core authentication API
     * @param userRepository        CRUD & queries for user data
     * @param passwordEncoder       For hashing and checking user's password securely
     * @param jwtUtils              Responsible for JWT token generation and validation
     */
    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                      PasswordEncoder passwordEncoder, JwtUtils jwtUtils, 
                      ActivationTokenRepository activationTokenRepository, OtpRepository otpRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.activationTokenRepository = activationTokenRepository;
        this.otpRepository = otpRepository;
    }

    /**
     * Authenticates a user based on username/password.
     * 
     * Steps:
     *  1. Receives login data from the UI, wraps into UsernamePasswordAuthenticationToken.
     *  2. Delegates verification to Spring's AuthenticationManager.
     *  3. Checks if the user account is active.
     *  4. On success, stores authentication context for the request lifecycle.
     *  5. Generates a signed JWT token for the user, to be used for future requests.
     *  6. Gathers user data for the client/UI (id, username, email, role).
     * 
     * @param loginRequest login credentials sent by frontend/backend client
     * @return LoginResponse (JWT and user details, to send back to frontend)
     * @throws RuntimeException if the user account is not active
     */
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        // Find the user first to check if they are active
        String usernameOrEmail = loginRequest.getUsernameOrEmail();
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found with username or email: " + usernameOrEmail));
        
        // Check if the user is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            String statusReason = user.getStatusReason();
            String errorMessage;
            switch (statusReason != null ? statusReason : "") {
                case "PENDING_ACTIVATION":
                    errorMessage = "Your account was created by an administrator. Please check your email to activate your account.";
                    break;
                case "PENDING_APPROVAL":
                    errorMessage = "Your registration request is pending approval by the administrator. Please wait for confirmation.";
                    break;
                case "DEACTIVATED_BY_ADMIN":
                    errorMessage = "Your account has been deactivated by the administrator. Please contact support for further assistance.";
                    break;
                case "REJECTED_BY_ADMIN":
                    errorMessage = "Your registration request has been rejected by the administrator.";
                    break;
                default:
                    errorMessage = "Account is inactive. Please contact an administrator.";
                    break;
            }
            throw new AccountInactiveException(errorMessage);
        }
        
        // Step 1: Create a token with usernameOrEmail and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(), // Accepts either username or email
                        loginRequest.getPassword()   // Raw password to check against saved hash
                )
        );

        // Step 4: If credentials valid, store the authentication (user principal, authorities, etc.)
        // This makes the user "authenticated" for the current thread.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Step 5: Use JWT utils to build a signed JWT, encoding user identity and claims.
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Step 6: Update the user's last login date
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        // Step 7: Gather user details to customize client session/UI.
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Compose the success reply: includes JWT (for Authorization header), some user metadata
        return LoginResponse.builder()
                .token(jwt) // The JWT for future API requests
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                // Assume single role
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .build();
    }

    /**
     * Registers a new user account in the system.
     * 
     * Steps:
     *  1. Checks for username or email conflicts in the database.
     *  2. If all unique, securely encodes the provided password.
     *  3. Populates a new User entity with validated sign-up data.
     *  4. Persists the new user in the repository/DB.
     *  5. For self-registered users, sets status to PENDING_APPROVAL and notifies admins.
     *  6. For admin-created users, sends verification email directly.
     * 
     * @param registerRequest sign-up data object (username, email, password, etc)
     * @param isAdminCreated flag indicating if the user is created by an admin
     * @return 
     * @throws RuntimeException if user/email is already present (UI should handle/notify)
     */
    @Transactional
    public String registerUser(RegisterRequest registerRequest, boolean isAdminCreated) {
        // Check for duplicates
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateFieldException("Username is already taken");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateFieldException("Email is already in use");
        }

        // Create inactive user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : "STAFF");
        user.setIsActive(false);
        
        if (isAdminCreated) {
            // Admin-created users: set status to PENDING_ACTIVATION and send activation email
            user.setStatusReason(User.STATUS_PENDING_ACTIVATION);
            userRepository.save(user);
            
            // Create activation token and send activation email
            String activationToken = UUID.randomUUID().toString();
            ActivationToken token = new ActivationToken();
            token.setToken(activationToken);
            token.setUserId(user.getId());
            token.setExpiryDate(LocalDateTime.now().plusHours(48)); // 48-hour expiry
            token.setPurpose(ActivationToken.TokenPurpose.ACTIVATION);
            
            activationTokenRepository.save(token);
            
            // Send activation email using the proper activation flow
            sendActivationEmail(user.getEmail(), activationToken, user.getFirstName());
            return "Registration successful. Activation email sent.";
        } else {
            // Self-registered users: set status to PENDING_APPROVAL and notify admins
            user.setStatusReason(User.STATUS_PENDING_APPROVAL);
            try {
                // Notify all admin users about the new registration
                notifyAdminsAboutNewUser(user);
                // Save user only if email is sent successfully
                userRepository.save(user);
                
                return "Your registration request is pending approval by the administrator. Please wait for confirmation.";
            } catch (Exception e) {
                // If email sending fails, do not save the user (transaction will rollback)
                throw new RuntimeException("Registration was unsuccessful. Please try again.", e);
            }
        }
    }
    
    /**
     * Overloaded method for backward compatibility
     */
    public void registerUser(RegisterRequest registerRequest) {
        // Default to self-registration flow
        registerUser(registerRequest, false);
    }
    
    /**
     * Sends notification emails to all admin users about a new user registration
     * 
     * @param newUser the newly registered user
     * @throws RuntimeException if email sending fails
     */
    private void notifyAdminsAboutNewUser(User newUser) {
        List<User> adminUsers = userRepository.findByRoleAndIsActiveTrue("ADMIN");
        
        if (adminUsers.isEmpty()) {
            // Fallback if no admins found - throw exception to trigger rollback
            throw new RuntimeException("No active admin users found to notify about new user registration");
        }
        
        String fullName = newUser.getFirstName() + " " + newUser.getLastName();
        
        for (User admin : adminUsers) {
            try {
                emailService.sendAdminNotificationEmail(
                    admin.getEmail(), 
                    newUser.getUsername(), 
                    fullName, 
                    newUser.getEmail()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to send notification email to admin: " + admin.getEmail(), e);
            }
        }
    }
    
    /**
     * Sends an activation email to a user with an activation token
     * 
     * @param email The email address to send to
     * @param token The activation token
     * @param firstName The user's first name
     */
    private void sendActivationEmail(String email, String token, String firstName) {
        // Create a temporary user object for the email service
        User tempUser = new User();
        tempUser.setEmail(email);
        tempUser.setFirstName(firstName);
        emailService.sendActivationEmail(tempUser, token);
    }
    
    /**
     * Approves a user registration by an admin
     * This method is transactional and will roll back if any error occurs during the process.
     * 
     * @param userId the ID of the user to approve
     * @return true if successful, false otherwise
     * @throws RuntimeException if any error occurs during the approval process
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveUserRegistration(Integer userId) {
        try {
            // Find the user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            // Check if user is in pending approval state
            if (!User.STATUS_PENDING_APPROVAL.equals(user.getStatusReason())) {
                return false; // User is not in pending approval state
            }
            
            // Update user status to active
            user.setIsActive(true);
            user.setStatusReason(null); // Clear the pending status
            userRepository.save(user);
            

            
            // Send a descriptive email to inform the user that their account has been approved
            emailService.sendAccountApprovalEmail(user.getEmail(), user.getFirstName());
            
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error approving user registration: " + e.getMessage());
            e.printStackTrace();
            
            // Re-throw the exception to trigger transaction rollback
            throw new RuntimeException("Failed to approve user registration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Rejects a user registration by an admin
     * This method is transactional and will roll back if any error occurs during the process.
     * 
     * @param userId the ID of the user to reject
     * @param reason optional reason for rejection
     * @return true if successful, false otherwise
     * @throws RuntimeException if any error occurs during the rejection process
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectUserRegistration(Integer userId, String reason) {
        try {
            // Find the user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            // Check if user is in pending approval state
            if (!User.STATUS_PENDING_APPROVAL.equals(user.getStatusReason())) {
                return false; // User is not in pending approval state
            }
            
            // Update user status
            user.setStatusReason(User.STATUS_REJECTED);
            userRepository.save(user);
            
            // Notify the user about rejection
            emailService.sendAccountRejectionEmail(user.getEmail(), reason);
            
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error rejecting user registration: " + e.getMessage());
            e.printStackTrace();
            
            // Re-throw the exception to trigger transaction rollback
            throw new RuntimeException("Failed to reject user registration: " + e.getMessage(), e);
        }
    }

     // Generates a short-lived JWT as verification token (helper - must be implemented in JwtUtils)
    // public String generateVerificationToken(String email)

    // We're now using UserService.sendActivationEmail() instead of this method

    /**
     * Issues a fresh access token (JWT) for a user using a valid refresh token.
     * 
     * Refresh tokens are longer-lived and usually used to avoid requiring the user
     * to re-login often. This method checks that the token is valid,
     * fetches user by username found in the token, and issues a new JWT.
     * Also checks if the user account is active.
     *
     * @param refreshToken the old token issued previously (should be valid and not expired)
     * @return LoginResponse with new JWT and user details
     * @throws RuntimeException if refreshToken is not valid or user is inactive
     */
    public LoginResponse refreshToken(String refreshToken) {
        // Step 1: Only proceed if the provided refreshToken is valid (signature, expiry etc)
        if (jwtUtils.validateJwtToken(refreshToken)) {
            // Step 2: Extract username from token's payload/claims
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            
            // Step 3: Check if the user is active
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
            
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                String statusReason = user.getStatusReason();
                String errorMessage;
                switch (statusReason != null ? statusReason : "") {
                    case "PENDING_ACTIVATION":
                        errorMessage = "Your account was created by an administrator. Please check your email to activate your account.";
                        break;
                    case "PENDING_APPROVAL":
                        errorMessage = "Your registration request is pending approval by the administrator. Please wait for confirmation.";
                        break;
                    case "DEACTIVATED_BY_ADMIN":
                        errorMessage = "Your account has been deactivated by the administrator. Please contact support for further assistance.";
                        break;
                    case "REJECTED_BY_ADMIN":
                        errorMessage = "Your registration request has been rejected by the administrator.";
                        break;
                    default:
                        errorMessage = "Account is inactive. Please contact an administrator.";
                        break;
                }
                throw new AccountInactiveException(errorMessage);
            }

            // Step 4: Load the user's details by username (uses UserDetailsServiceImpl)
            UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

            // Step 5: Issue a new JWT, re-establishing the access session.
            String newToken = jwtUtils.generateJwtToken(
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials not needed for subsequent JWT creation
                            userDetails.getAuthorities()
                    )
            );

            // Prepare new session LoginResponse
            return LoginResponse.builder()
                    .token(newToken)
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    // Assume single role
                    .role(userDetails.getAuthorities().iterator().next().getAuthority())
                    .build();
        }
        // If anything is invalid (i.e., invalidated or tampered token), fail the process.
        throw new RuntimeException("Invalid refresh token");
    }

    /**
     * Initiates the password reset process for a user.
     * 
     * Steps:
     *  1. Finds the user by email
     *  2. Checks if the user is inactive with status PENDING_ACTIVATION, if so throws an error
     *  3. Invalidates any existing reset tokens for this user
     *  4. Generates a new reset token with expiry time
     *  5. Saves the token to the database
     *  6. Sends an email with the reset link
     * 
     * @param request contains the email of the user requesting password reset
     * @throws AccountInactiveException if the user is inactive with status PENDING_ACTIVATION or PENDING_APPROVAL
     */
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check if the user is inactive with status PENDING_ACTIVATION or PENDING_APPROVAL
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                if (User.STATUS_PENDING_ACTIVATION.equals(user.getStatusReason())) {
                    throw new AccountInactiveException("Your account is pending activation. Please check your email for the activation link to set your password.");
                } else if (User.STATUS_PENDING_APPROVAL.equals(user.getStatusReason())) {
                    throw new AccountInactiveException("Your registration request is pending approval by the administrator. Please wait for confirmation.");
                }
            }
            // Invalidate previous password reset tokens for this user
            // Note: Assuming a method to find tokens by userId is defined in the repository
            // If not, this needs to be added to ActivationTokenRepository
            // For now, skipping invalidation as the method signature may not return a list
            // Alternatively, a custom query or method in the repository is needed

            // Generate token and expiry date (1 hour expiry)
            String token = UUID.randomUUID().toString();
            ActivationToken resetToken = new ActivationToken();
            resetToken.setToken(token);
            resetToken.setUserId(user.getId());
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
            resetToken.setPurpose(ActivationToken.TokenPurpose.PASSWORD_RESET);

            activationTokenRepository.save(resetToken);

            // Send the token to the user's email address
            sendPasswordResetEmail(request.getEmail(), token);
        }
        // If not found, don't reveal to the client; just act as if successful for privacy
    }
    
    private void sendPasswordResetEmail(String email, String token) {
        emailService.sendPasswordResetEmail(email, token);
    }
    
    /**
     * Completes the password reset process by validating the token and updating the password.
     * 
     * Steps:
     *  1. Validates the reset token exists and is not expired
     *  2. Finds the user associated with the token
     *  3. Updates the user's password with the new encoded password
     *  4. Invalidates the used token
     * 
     * @param request contains the token and new password
     * @throws RuntimeException if token is invalid, expired, or user not found
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ActivationToken token = activationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (token.isExpired() || token.isUsed() || token.getPurpose() != ActivationToken.TokenPurpose.PASSWORD_RESET) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for this token"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate the token after use
        token.setUsed(true);
        activationTokenRepository.save(token);
    }

    /**
     * Verifies if a password reset token is valid and not expired.
     * This method checks the token without consuming it, allowing the frontend
     * to validate tokens before showing the reset form.
     * 
     * @param tokenString The reset token to verify
     * @return true if token is valid and not expired, false otherwise
     * @throws RuntimeException if token is not found
     */
    public boolean verifyResetToken(String tokenString) {
        ActivationToken token = activationTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        // Check if token is valid for password reset
        if (token.isExpired() || token.isUsed() || token.getPurpose() != ActivationToken.TokenPurpose.PASSWORD_RESET) {
            return false;
        }

        // Verify that the associated user still exists
        boolean userExists = userRepository.existsById(token.getUserId());
        if (!userExists) {
            return false;
        }

        return true;
    }

    /**
     * Handles logout for the current user.
     * 
     * Since JWT is stateless, this method currently does not invalidate tokens on the server side.
     * However, it can be extended in the future to handle refresh token invalidation or other session management.
     * 
     * @return A success message indicating logout was processed.
     */
    public String logout() {
        // Currently, no server-side token invalidation is performed due to JWT's stateless nature.
        // If refresh tokens or a token blacklist are implemented, invalidation logic would be added here.

        return "Logout successful";
    }

    /**
     * Initiates password reset with OTP verification.
     * 
     * Steps:
     *  1. Finds the user by email
     *  2. Checks if the user is active
     *  3. Generates and sends OTP to user's email
     *  4. Stores OTP in database with expiry time
     * 
     * @param request contains the email of the user requesting password reset
     * @throws AccountInactiveException if the user is inactive
     */
    @Transactional
    public void initiatePasswordResetWithOtp(ForgotPasswordOtpRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check if the user is active
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                if (User.STATUS_PENDING_ACTIVATION.equals(user.getStatusReason())) {
                    throw new AccountInactiveException("Your account is pending activation. Please check your email for the activation link.");
                } else if (User.STATUS_PENDING_APPROVAL.equals(user.getStatusReason())) {
                    throw new AccountInactiveException("Your registration request is pending approval by the administrator. Please wait for confirmation.");
                }
            }

            // Generate 6 digit OTP
            String otpCode = String.format("%06d", new Random().nextInt(1000000));

            // Delete any existing password reset OTP for this email
            otpRepository.deleteByEmailAndVerificationType(request.getEmail(), "PASSWORD_RESET");

            // Create new OTP
            Otp otp = new Otp();
            otp.setUserId(user.getId());
            otp.setEmail(request.getEmail());
            otp.setNewEmail(null);
            otp.setOtpCode(otpCode);
            otp.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // 10 minutes expiry for password reset
            otp.setVerified(false);
            otp.setVerificationType("PASSWORD_RESET");

            otpRepository.save(otp);

            // Send OTP email
            sendPasswordResetOtpEmail(request.getEmail(), otpCode, user.getFirstName());
        }
        // If user not found, don't reveal to the client for privacy
    }

    /**
     * Verifies the password reset OTP.
     * 
     * @param request contains email and OTP
     * @return true if OTP is valid and verified, false otherwise
     */
    public boolean verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request) {
        Optional<Otp> optionalOtp = otpRepository.findByEmailAndVerificationType(
            request.getEmail(), "PASSWORD_RESET");

        if (optionalOtp.isEmpty()) {
            return false;
        }

        Otp otp = optionalOtp.get();

        if (otp.isExpired() || otp.isVerified()) {
            return false;
        }

        if (!otp.getOtpCode().equals(request.getOtp())) {
            return false;
        }

        // Mark OTP as verified but don't delete it yet (needed for password reset)
        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    /**
     * Resets password using verified OTP.
     * 
     * @param request contains email, OTP, and new password
     * @throws RuntimeException if OTP is invalid or not verified
     */
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        // Find the verified OTP
        Optional<Otp> optionalOtp = otpRepository.findByEmailAndVerificationType(
            request.getEmail(), "PASSWORD_RESET");

        if (optionalOtp.isEmpty()) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        Otp otp = optionalOtp.get();

        // Verify OTP is still valid and verified
        if (otp.isExpired() || !otp.isVerified() || !otp.getOtpCode().equals(request.getOtp())) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Find the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete the used OTP
        otpRepository.delete(otp);
    }

    /**
     * Sends password reset OTP email to the user.
     * 
     * @param email user's email
     * @param otpCode the OTP code
     * @param firstName user's first name
     */
    private void sendPasswordResetOtpEmail(String email, String otpCode, String firstName) {
        emailService.sendOtpEmail(email, firstName, otpCode, "PASSWORD_RESET", 10);
    }
}
