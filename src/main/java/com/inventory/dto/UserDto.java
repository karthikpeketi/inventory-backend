package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserDto – Data Transfer Object representing user account data.
 * 
 * Purpose:
 * - Used in API responses to send or receive user details (e.g., for profile, admin directories, etc.).
 * - Separates transport logic from entity (database) logic, limiting data exposure and security risk.
 *
 * Lombok:
 * - @Data: Auto-generates all the standard getters/setters and utility methods.
 * - @Builder: Enables "builder pattern" creation (UserDto.builder()...), simplifies object creation in service/business code.
 * - @NoArgsConstructor / @AllArgsConstructor: Constructors for serialization and mapping layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    // Unique identifier for the user (internal primary key)
    private Integer id;

    // Username, usually unique, for login and display purposes
    private String username;

    // Email address of the user, should be unique in most systems
    private String email;

    // First/Given name of the user. Optional.
    private String firstName;

    // Last/Family name of the user. Optional.
    private String lastName;

    // Role of this user in the system (e.g., "ADMIN", "STAFF"). This governs authorization and permissions.
    private String role;

    // Indicates whether the user account is currently active/enabled.
    private Boolean isActive;
    
    // Reason for the current status (e.g., "PENDING_ACTIVATION", "PENDING_APPROVAL", "DEACTIVATED_BY_ADMIN")
    private String statusReason;
    

    
    // Flag to indicate if this is a self-registration requiring approval
    private Boolean requiresApproval;

    // NOTE: Including plain/password fields in DTOs sent to UI is generally a security risk.
    // In real-world APIs, omit password or only allow via write-only DTOs used for registration/reset flows.
    // Used here for flexibility but should be removed/hidden in most profile/expose-to-client contexts.
    private String password;

    // Timestamp of when the user was created (for display, audit)
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;

    // Timestamp of last profile/account update (for display, audit, and concurrency)
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    
    // Timestamp of last login
    private LocalDateTime lastLoginDate;
    
    // Formatted date strings for direct display in UI
    private String createdAtFormatted;
    private String updatedAtFormatted;
    private String lastLoginDateFormatted;
}
