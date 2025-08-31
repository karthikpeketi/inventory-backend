package com.inventory.repository;

import com.inventory.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository – Spring Data JPA repository for User entities.
 *
 * Main concepts for learners:
 * - By extending JpaRepository, this interface provides ready-made CRUD operations, pagination and 
 *   can auto-generate queries based on method names.
 * - JpaSpecificationExecutor enables dynamic query building for complex search operations.
 * - @Repository marks it for Spring's component scan (needed for dependency injection, exception translation, etc).
 * - These repository interfaces do NOT need implementation classes; Spring Data proxies them at runtime!
 *
 * Optional: Used for null-safety (no result returns Optional.empty(), rather than null).
 * Boolean: For fast existence checks without loading the object itself.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    
    /**
     * Find a user by their unique username.
     * Method name is auto-parsed by Spring Data JPA to generate a query like:
     * "SELECT u FROM User u WHERE u.username = :username"
     * @param username login name to look up
     * @return Optional containing User if found, or empty if not present
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Lookup user by email address (should also be unique per business constraints).
     * @param email Email string to search for
     * @return Optional User (empty if not present)
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if there is at least one user by this username.
     * Convenience for validation (e.g., during registration).
     * @param username the username to verify
     * @return true if a user exists, false if not
     */
    Boolean existsByUsername(String username);
    
    /**
     * Checks if a user by this email is already present (for login/registration checks)
     * @param email email to check
     * @return true if user by this email exists
     */
    Boolean existsByEmail(String email);
    
    /**
     * Find users by status reason
     * @param statusReason the status reason to filter by
     * @return List of users with the specified status reason
     */
    List<User> findByStatusReason(String statusReason);
    
    /**
     * Find all users with admin role
     * @return List of users with admin role
     */
    List<User> findByRoleAndIsActiveTrue(String role);

    // Search methods for global search functionality
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);
    
    // Search methods for global search functionality - only active users
    List<User> findByIsActiveTrueAndFirstNameContainingIgnoreCaseOrIsActiveTrueAndLastNameContainingIgnoreCaseOrIsActiveTrueAndEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);
}