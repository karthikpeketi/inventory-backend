package com.inventory.service;

import com.inventory.dto.UserDto;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.ActivationToken;
import com.inventory.model.Otp;
import com.inventory.model.User;
import com.inventory.repository.ActivationTokenRepository;
import com.inventory.repository.OtpRepository;
import com.inventory.repository.UserRepository;
import com.inventory.util.ModelMapper;
import com.inventory.exception.DuplicateFieldException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UserService
 *
 * <p>
 * Encapsulates all operations for creating, retrieving, updating, and deleting users.
 * Enforces validation on usernames and email for uniqueness. Ensures passwords are securely hashed.
 * Maps between domain model User and transfer-friendly UserDto objects for API responses.
 * </p>
 */
@Service // Mark as a Spring-managed service bean for auto-wiring and dependency injection
public class UserService {
    // Dependency for persistent storage of users
    private final UserRepository userRepository;
    // Secure password hashing (BCrypt, injected by Spring Security config)
    private final PasswordEncoder passwordEncoder;
    // Repository for activation tokens
    private final ActivationTokenRepository activationTokenRepository;
    // Repository for OTPs
    private final OtpRepository otpRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Constructor-based dependency injection for repositories and services.
     * This style allows for easier testing (with mocks) and clear immutability.
     */


    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      ActivationTokenRepository activationTokenRepository,
                      OtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activationTokenRepository = activationTokenRepository;
        this.otpRepository = otpRepository;
    }

    /**
     * Get all users with pagination and sorting
     * @param isActive Filter by active status (true = only active users, null = all users)
     * @param statusReason Filter by status reason (e.g., PENDING_APPROVAL, PENDING_ACTIVATION, DEACTIVATED)
     * @param pageable Pagination and sorting information
     * @return Page of users
     */
    public Page<UserDto> getAllUsers(Boolean isActive, String statusReason, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        
        // Add isActive filter if provided
        if (isActive != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("isActive"), isActive));
        }
        
        // Add statusReason filter if provided
        if (statusReason != null && !statusReason.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("statusReason"), statusReason));
        }
        
        // Apply the specification
        if (spec != null) {
            return userRepository.findAll(spec, pageable)
                    .map(ModelMapper::mapToUserDto);
        } else {
            // Return all users if no filters are applied
            return userRepository.findAll(pageable)
                    .map(ModelMapper::mapToUserDto);
        }
    }

    /**
     * Get all users without pagination (for backward compatibility)
     * @return List of all users
     */
    public List<UserDto> getAllUsersWithoutPagination() {
        return userRepository.findAll()
                .stream()
                .map(ModelMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    /**
     * Search users by name or email
     * @param searchTerm Search term
     * @param searchFields Fields to search in
     * @param isActive Filter by active status (true = only active users, null = all users)
     * @param statusReason Filter by status reason (e.g., PENDING_APPROVAL, PENDING_ACTIVATION, DEACTIVATED)
     * @param pageable Pagination and sorting information
     * @return Page of matching users
     */
    public Page<UserDto> searchUsers(String searchTerm, String[] searchFields, Boolean isActive, String statusReason, Pageable pageable) {
        // Create a specification for the search
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> searchPredicates = new ArrayList<>();
            List<Predicate> finalPredicates = new ArrayList<>();
            
            // Create OR predicates for each search field
            for (String field : searchFields) {
                searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(field)), 
                    "%" + searchTerm.toLowerCase() + "%"
                ));
            }
            
            // Combine search predicates with OR
            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            finalPredicates.add(searchPredicate);
            
            // Add isActive filter if provided
            if (isActive != null) {
                finalPredicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }
            
            // Add statusReason filter if provided
            if (statusReason != null && !statusReason.isEmpty()) {
                finalPredicates.add(criteriaBuilder.equal(root.get("statusReason"), statusReason));
            }
            
            // Combine all predicates with AND
            return criteriaBuilder.and(finalPredicates.toArray(new Predicate[0]));
        };
        
        return userRepository.findAll(spec, pageable)
                .map(ModelMapper::mapToUserDto);
    }

    /**
     * Lookup a single user by database ID and return as a DTO.
     * Throws a 404-style custom exception if not found.
     */
    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ModelMapper.mapToUserDto(user);
    }

    /**
     * Create a new user account, performing thorough validation before persistence.
     * - Checks for username and email uniqueness.
     * - Maps DTO to entity, hashes the password (if provided), then saves.
     * - For admin-created users (isActive=false), sends an activation email.
     *
     * @param userDto Contains details for the new user
     * @return UserDto for the newly created user (with DB-generated fields)
     */
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // Prevent duplicate usernames
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Prevent duplicate emails
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Construct entity from DTO
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        
        // Handle password based on activation flow
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            // If password is provided, hash it (for self-registration or direct creation)
            user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        } else {
            // For admin-created users without passwords, set a placeholder
            // This will be replaced when the user activates their account
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        }
        
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setRole(userDto.getRole());
        
        // Set isActive based on DTO or default to false for admin-created users without passwords
        if (userDto.getIsActive() != null) {
            user.setIsActive(userDto.getIsActive());
        } else {
            user.setIsActive(userDto.getPassword() != null && !userDto.getPassword().isEmpty());
        }
        
        // Set statusReason based on the context
        if (userDto.getStatusReason() != null) {
            user.setStatusReason(userDto.getStatusReason());
        } else if (!Boolean.TRUE.equals(user.getIsActive())) {
            // If user is not active and no specific reason is provided
            if (userDto.getPassword() == null || userDto.getPassword().isEmpty()) {
                // Admin-created user without password
                user.setStatusReason(User.STATUS_PENDING_ACTIVATION);
            } else if (userDto.getRequiresApproval() != null && userDto.getRequiresApproval()) {
                // Self-registered user waiting for approval
                user.setStatusReason(User.STATUS_PENDING_APPROVAL);
            } else {
                // Default for inactive users
                user.setStatusReason(User.STATUS_PENDING_ACTIVATION);
            }
        }

        // Save user
        User savedUser = userRepository.save(user);
        

        
        // If user is not active (admin-created), send activation email
        if (!Boolean.TRUE.equals(savedUser.getIsActive()) && 
            User.STATUS_PENDING_ACTIVATION.equals(savedUser.getStatusReason())) {
            sendActivationEmail(savedUser.getId());
        }
        
        return ModelMapper.mapToUserDto(savedUser);
    }

    /**
     * Update an existing user's details. Uniqueness for username/email is enforced.
     * If password is present and non-empty in the DTO, it will be hashed and updated.
     * Only updates fields that are present in the DTO (not null).
     * 
     * @param id which user to update
     * @param userDto new values (ignores null/empty password for no-password change)
     * @return updated user data as DTO
     */
    public UserDto updateUser(Integer id, UserDto userDto) {
        // Lookup entity or fail
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if this is a partial update (only updating isActive)
        boolean isPartialUpdate = userDto.getUsername() == null && 
                                 userDto.getEmail() == null && 
                                 userDto.getFirstName() == null && 
                                 userDto.getLastName() == null && 
                                 userDto.getRole() == null && 
                                 userDto.getPassword() == null;

        if (!isPartialUpdate) {
            // Make sure username and email are still unique if changed
            if (userDto.getUsername() != null && !user.getUsername().equals(userDto.getUsername()) && 
                userRepository.existsByUsername(userDto.getUsername())) {
                throw new RuntimeException("Username is already taken");
            }

            if (userDto.getEmail() != null && !user.getEmail().equals(userDto.getEmail()) && 
                userRepository.existsByEmail(userDto.getEmail())) {
                throw new RuntimeException("Email is already in use");
            }

            // Update fields with new values only if they are provided
            if (userDto.getUsername() != null) {
                user.setUsername(userDto.getUsername());
            }
            
            if (userDto.getEmail() != null) {
                user.setEmail(userDto.getEmail());
            }

            // Only update password if field is set
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
            }
            
            if (userDto.getFirstName() != null) {
                user.setFirstName(userDto.getFirstName());
            }
            
            if (userDto.getLastName() != null) {
                user.setLastName(userDto.getLastName());
            }
            
            if (userDto.getRole() != null) {
                user.setRole(userDto.getRole());
            }
        }
        
        // Always update isActive if it's provided
        if (userDto.getIsActive() != null) {
            user.setIsActive(userDto.getIsActive());
        }
        
        // Update statusReason if provided
        if (userDto.getStatusReason() != null) {
            user.setStatusReason(userDto.getStatusReason());
        } else if (userDto.getIsActive() != null) {
            // If isActive is being updated but no statusReason is provided
            if (Boolean.TRUE.equals(userDto.getIsActive())) {
                // If activating the user, clear the status reason
                user.setStatusReason(null);
            }
        }

        // Save and return updated user
        User updatedUser = userRepository.save(user);
        return ModelMapper.mapToUserDto(updatedUser);
    }

    /**
     * Deletes a user record from the database (hard delete).
     * Throws if the user does not exist (guards against silent errors).
     *
     * @param id User database id to delete
     */
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    /**
     * Creates an activation token for a user and sends an activation email
     * 
     * @param userId The ID of the user to send the activation email to
     * @return The generated activation token
     */
    @Transactional
    public String sendActivationEmail(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check if user is already active
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("User is already active");
        }
        
        // Check if user is in a state that requires activation
        if (!User.STATUS_PENDING_ACTIVATION.equals(user.getStatusReason())) {
            throw new RuntimeException("User is not in a state that requires activation");
        }
        
        // Invalidate any existing tokens for this user
        activationTokenRepository.findByUserId(userId)
                .ifPresent(token -> activationTokenRepository.deleteById(token.getId()));
        
        // Generate a new token
        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(token);
        activationToken.setUserId(userId);
        activationToken.setExpiryDate(LocalDateTime.now().plusHours(48)); // 48-hour expiry
        
        activationTokenRepository.save(activationToken);
        
        // Send activation email
        sendUserActivationEmail(user.getEmail(), token, user.getFirstName());
        
        return token;
    }
    
    /**
     * Verify an activation token and return the associated user
     * 
     * @param token The activation token to verify
     * @return The user associated with the token
     */
    public UserDto verifyActivationToken(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));
        
        if (activationToken.isExpired()) {
            throw new RuntimeException("Activation token has expired");
        }
        
        if (activationToken.isUsed()) {
            throw new RuntimeException("Activation token has already been used");
        }
        
        User user = userRepository.findById(activationToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for this token"));
        
        return ModelMapper.mapToUserDto(user);
    }
    
    /**
     * Activate a user account with a token and set their password
     * 
     * @param token The activation token
     * @param password The new password to set
     */
    @Transactional
    public void activateAccount(String token, String password) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));
        
        if (activationToken.isExpired()) {
            throw new RuntimeException("Activation token has expired");
        }
        
        if (activationToken.isUsed()) {
            throw new RuntimeException("Activation token has already been used");
        }
        
        User user = userRepository.findById(activationToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for this token"));
        
        // Update user password and set active
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setStatusReason(null);
        userRepository.save(user);
        
        // Mark token as used
        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
    }
    
    /**
     * Send an activation email to a user
     * 
     * @param email The email address to send to
     * @param token The activation token
     * @param firstName The user's first name
     */
    private void sendUserActivationEmail(String email, String token, String firstName) {
        emailService.sendAccountActivationEmail(email, firstName, token);
    }

    // New methods for profile update

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Transactional
    public void sendCurrentEmailOtp(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate 6 digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(1000000));

        Otp otp = new Otp();
        otp.setUserId(userId);
        otp.setEmail(user.getEmail());
        otp.setNewEmail(null);
        otp.setOtpCode(otpCode);
        otp.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        otp.setVerificationType("CURRENT_EMAIL");

        // Delete any existing current email OTP for this user
        otpRepository.deleteByUserIdAndVerificationType(userId, "CURRENT_EMAIL");

        otpRepository.save(otp);

        // Send email to current email
        sendOtpEmail(user.getEmail(), otpCode, user.getFirstName(), "current");
    }

    @Transactional
    public void sendEmailChangeOtp(Integer userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmail().equals(newEmail)) {
            throw new RuntimeException("New email is the same as current");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new DuplicateFieldException("Email already in use");
        }

        // Check if current email is verified first
        Optional<Otp> currentEmailOtp = otpRepository.findByUserIdAndEmailAndVerificationType(
            userId, user.getEmail(), "CURRENT_EMAIL");
        
        if (currentEmailOtp.isEmpty() || !currentEmailOtp.get().isVerified()) {
            throw new RuntimeException("Current email must be verified first");
        }

        // Generate 6 digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(1000000));

        Otp otp = new Otp();
        otp.setUserId(userId);
        otp.setEmail(newEmail);
        otp.setNewEmail(newEmail);
        otp.setOtpCode(otpCode);
        otp.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        otp.setVerificationType("NEW_EMAIL");

        // Delete any existing new email OTP for this user
        otpRepository.deleteByUserIdAndVerificationType(userId, "NEW_EMAIL");

        otpRepository.save(otp);

        // Send email to new email
        sendOtpEmail(newEmail, otpCode, user.getFirstName(), "new");
    }

    private void sendOtpEmail(String email, String otpCode, String firstName, String emailType) {
        String purpose = "current".equals(emailType) ? "CURRENT_EMAIL" : "NEW_EMAIL";
        emailService.sendOtpEmail(email, firstName, otpCode, purpose, 5);
    }

    public boolean verifyCurrentEmailOtp(Integer userId, String email, String otpCode) {
        Optional<Otp> optionalOtp = otpRepository.findByUserIdAndEmailAndVerificationType(
            userId, email, "CURRENT_EMAIL");

        if (optionalOtp.isEmpty()) {
            return false;
        }

        Otp otp = optionalOtp.get();

        if (otp.isExpired() || otp.isVerified()) {
            return false;
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            return false;
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    public boolean verifyEmailChangeOtp(Integer userId, String newEmail, String otpCode) {
        Optional<Otp> optionalOtp = otpRepository.findByUserIdAndEmailAndVerificationType(
            userId, newEmail, "NEW_EMAIL");

        if (optionalOtp.isEmpty()) {
            return false;
        }

        Otp otp = optionalOtp.get();

        if (otp.isExpired() || otp.isVerified()) {
            return false;
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            return false;
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    public UserDto selfUpdateUser(Integer id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update firstName
        if (userDto.getFirstName() != null) {
            user.setFirstName(userDto.getFirstName());
        }

        // Update lastName
        if (userDto.getLastName() != null) {
            user.setLastName(userDto.getLastName());
        }

        // Update username
        if (userDto.getUsername() != null) {
            if (!user.getUsername().equals(userDto.getUsername()) && userRepository.existsByUsername(userDto.getUsername())) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(userDto.getUsername());
        }

        // Update email with two-step OTP verification
        if (userDto.getEmail() != null && !user.getEmail().equals(userDto.getEmail())) {
            // Check current email verification
            Optional<Otp> currentEmailOtp = otpRepository.findByUserIdAndEmailAndVerificationType(
                id, user.getEmail(), "CURRENT_EMAIL");
            
            if (currentEmailOtp.isEmpty() || !currentEmailOtp.get().isVerified()) {
                throw new RuntimeException("Current email verification required");
            }

            // Check new email verification
            Optional<Otp> newEmailOtp = otpRepository.findByUserIdAndEmailAndVerificationType(
                id, userDto.getEmail(), "NEW_EMAIL");

            if (newEmailOtp.isEmpty() || !newEmailOtp.get().isVerified()) {
                throw new RuntimeException("New email verification required");
            }

            user.setEmail(userDto.getEmail());

            // Clean up both OTPs
            otpRepository.delete(currentEmailOtp.get());
            otpRepository.delete(newEmailOtp.get());
        }

        // Password update not allowed here

        User updated = userRepository.save(user);
        return ModelMapper.mapToUserDto(updated);
    }

    /**
     * Change password for the authenticated user
     * @param userId The ID of the user changing password
     * @param currentPassword The current password for verification
     * @param newPassword The new password to set
     * @throws RuntimeException if current password is incorrect
     */
    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Ensure new password is different from current password
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
