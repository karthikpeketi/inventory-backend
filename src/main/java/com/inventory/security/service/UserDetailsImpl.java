
package com.inventory.security.service;

import com.inventory.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * UserDetailsImpl
 *
 * <p>
 * This class is a concrete implementation of Spring Security's {@link UserDetails} interface.
 * It represents the authenticated user details that Spring Security uses during authentication and authorization.
 * </p>
 * <p>
 * This class adapts your application's domain User object into a format understandable by Spring Security.
 * </p>
 */
public class UserDetailsImpl implements UserDetails {
    // Used during serialization (such as caching and session storage)
    private static final long serialVersionUID = 1L;

    // Fields representing the essential user information
    private Integer id;          // Unique identifier of the user
    private String username;     // Username for authentication
    private String email;        // Email address
    private String password;     // Password hash (never plain text!)
    private Boolean isActive;    // Whether the user account is active
    // Collection of user authorities, typically roles and permissions
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor for fully creating a UserDetailsImpl object.
     * All essential fields are provided for security operations.
     */
    public UserDetailsImpl(Integer id, String username, String email, String password,
                          Collection<? extends GrantedAuthority> authorities, Boolean isActive) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.isActive = isActive;
    }

    /**
     * Static factory method to build a UserDetailsImpl instance from a domain User object.
     * <p>
     * This helps keep separation between your application entities and security logic.
     * </p>
     * <p>
     * It creates a single authority of form "ROLE_{role}" (required by Spring Security conventions).
     * The authority list is immutable and contains just one role.
     * </p>
     * @param user The domain user entity
     * @return Instance of UserDetailsImpl representing this user for the security layer
     */
    public static UserDetailsImpl build(User user) {
        // Create a list with a single role authority (Spring Security expects roles to start with "ROLE_")
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        // Map all required fields from the domain User model
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(), // Store hashed password only!
                authorities,
                user.getIsActive()
        );
    }

    /**
     * Returns the authorities granted to the user, used by Spring Security for authorization.
     * @return collection of granted authorities (typically just roles; never null)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Get the user database ID.
     * Not required by the UserDetails interface, but useful for custom logic in the application.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the user's email.
     * Not strictly part of Spring Security, but available for convenience or further profile data.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's hashed password.
     * Used internally by Spring Security for authentication (password matching).
     * @return hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the user's principal username.
     * Used by Spring Security for login authentication and token subject claims.
     * @return the user's username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Indicates whether the user's account has expired.
     * You can extend this to dynamically check expiry dates from your user model.
     * Returning true means the account is always non-expired.
     *
     * @return true if account is not expired
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     * Enable account locking logic as needed.
     * Always true here (the user is not locked).
     *
     * @return true if account is not locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) are expired.
     * Could be used to enforce password rotation policies.
     * Always true here (never expired).
     *
     * @return true if credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled.
     * Connected to the isActive flag in the User model.
     * Only returns true if the user account is active.
     *
     * @return true if the user is active/enabled
     */
    @Override
    public boolean isEnabled() {
        return isActive != null && isActive;
    }

    /**
     * Custom equality check.
     * Two UserDetailsImpl instances are considered equal if they have the same ID.
     *
     * @param o the object to compare
     * @return true if both have the same database ID; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        // Standard equality self-check
        if (this == o)
            return true;
        // Null and class type check
        if (o == null || getClass() != o.getClass())
            return false;
        // Field comparison for id
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}
