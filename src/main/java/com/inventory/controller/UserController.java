package com.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.dto.UserDto;
import com.inventory.dto.ActivateAccountRequest;
import com.inventory.dto.ChangePasswordRequest;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import com.inventory.service.AuthService;
import com.inventory.service.UserService;
import com.inventory.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get users with pagination, sorting, and filtering (ADMIN only)
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Field to sort by
     * @param direction Sort direction ('asc' or 'desc')
     * @param search Search query
     * @param searchFields Fields to search in (comma-separated)
     * @param isActive Filter by active status (true = only active users, null = all users)
     * @return paginated users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "firstName,lastName,email") String searchFields,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "false") Boolean pendingApproval) {
        
        try {
            // Create pageable object with sorting
            Pageable pageable;
            if (sort != null && !sort.isEmpty()) {
                Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Direction.DESC : Direction.ASC;
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            } else {
                pageable = PageRequest.of(page, size);
            }
            
            // Get users with pagination, sorting, and filtering
            Page<UserDto> users;
            if (search != null && !search.isEmpty()) {
                users = userService.searchUsers(search, searchFields.split(","), isActive, pendingApproval ? User.STATUS_PENDING_APPROVAL : null, pageable);
            } else {
                users = userService.getAllUsers(isActive, pendingApproval ? User.STATUS_PENDING_APPROVAL : null, pageable);
            }
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get all users without pagination (ADMIN only)
     * For backward compatibility
     * @return list of users
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsersWithoutPagination();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID (ADMIN only)
     * @param id user ID
     * @return user
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Create a new user (ADMIN only)
     * @param userDto user data
     * @return created user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);
        return ResponseEntity.ok(createdUser);
    }
    
    /**
     * Update a user (ADMIN only)
     * @param id user ID
     * @param userDto user data
     * @return updated user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Integer id, @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Delete a user (ADMIN only)
     * @param id user ID
     * @return success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().body(new MessageResponse("User deleted successfully"));
    }
    
    /**
     * Deactivate a user (ADMIN only)
     * @param id user ID
     * @return success message
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Integer id) {
        try {
            UserDto userDto = new UserDto();
            userDto.setIsActive(false);
            userDto.setStatusReason(User.STATUS_DEACTIVATED);
            UserDto updatedUser = userService.updateUser(id, userDto);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully", "user", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Reactivate a user (ADMIN only)
     * @param id user ID
     * @return success message
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reactivateUser(@PathVariable Integer id) {
        try {
            UserDto userDto = new UserDto();
            userDto.setIsActive(true);
            userDto.setStatusReason(null);
            UserDto updatedUser = userService.updateUser(id, userDto);
            return ResponseEntity.ok(Map.of("message", "User reactivated successfully", "user", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Send activation email to a user (ADMIN only)
     * @param id user ID
     * @return success message
     * @deprecated Use /{id}/send-activation-email instead
     */
    @Deprecated
    @PostMapping("/{id}/send-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendActivationEmailLegacy(@PathVariable Integer id) {
        try {
            userService.sendActivationEmail(id);
            return ResponseEntity.ok(new MessageResponse("Activation email sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Verify an activation token
     * @param token Activation token
     * @return User information if token is valid
     */
    @GetMapping("/verify-activation-token")
    public ResponseEntity<?> verifyActivationToken(@RequestParam String token) {
        try {
            UserDto user = userService.verifyActivationToken(token);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Activate a user account with a token and set password
     * @param request Activation request containing token and password
     * @return Success message
     */
    @PostMapping("/activate-account")
    public ResponseEntity<?> activateAccount(@RequestBody ActivateAccountRequest request) {
        try {
            userService.activateAccount(request.getToken(), request.getPassword());
            return ResponseEntity.ok(Map.of("message", "Account activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Send activation email to a user
     * @param id User ID
     * @return Success message
     */
    @PostMapping("/{id}/send-activation-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendActivationEmail(@PathVariable Integer id) {
        try {
            userService.sendActivationEmail(id);
            return ResponseEntity.ok(Map.of("message", "Activation email sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Approve a user registration
     * @param id User ID
     * @return Success message
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveUser(@PathVariable Integer id) {
        try {
            // Use the AuthService to approve the user and send verification email
            boolean success = authService.approveUserRegistration(id);
            
            if (success) {
                // Get updated user info
                UserDto updatedUser = userService.getUserById(id);
                return ResponseEntity.ok(Map.of(
                    "message", "User approved successfully. Verification email has been sent to the user.", 
                    "user", updatedUser
                ));
            } else {
                return ResponseEntity.badRequest().body(
                    new MessageResponse("User is not in pending approval state")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Reject a user registration
     * @param id User ID
     * @param reason Optional reason for rejection
     * @return Success message
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectUser(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {
        try {
            // Use the AuthService to reject the user and notify them
            boolean success = authService.rejectUserRegistration(id, reason);
            
            if (success) {
                // Get updated user info
                UserDto updatedUser = userService.getUserById(id);
                return ResponseEntity.ok(Map.of(
                    "message", "User rejected successfully. The user has been notified.", 
                    "user", updatedUser
                ));
            } else {
                return ResponseEntity.badRequest().body(
                    new MessageResponse("User is not in pending approval state")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // New endpoints for profile update

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.isUsernameAvailable(username));
    }

    @PostMapping("/send-current-email-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendCurrentEmailOtp() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            userService.sendCurrentEmailOtp(user.getId());
            return ResponseEntity.ok(Map.of("message", "OTP sent to current email successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String newEmail = request.get("newEmail");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userService.sendEmailChangeOtp(user.getId(), newEmail);
        return ResponseEntity.ok(Map.of("message", "OTP sent to new email successfully"));
    }

    @PostMapping("/verify-current-email-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> verifyCurrentEmailOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean verified = userService.verifyCurrentEmailOtp(user.getId(), email, otp);
        return ResponseEntity.ok(verified);
    }

    @PostMapping("/verify-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> request) {
        String newEmail = request.get("newEmail");
        String otp = request.get("otp");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean verified = userService.verifyEmailChangeOtp(user.getId(), newEmail, otp);
        return ResponseEntity.ok(verified);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateSelf(@RequestBody UserDto userDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserDto updated = userService.selfUpdateUser(user.getId(), userDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Change password for the authenticated user
     * @param request Change password request containing current and new password
     * @return Success message
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    // Helper class for response messages
    class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
