package com.inventory.security.service;

import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl
 *
 * <p>
 * Implementation of Spring Security's {@link UserDetailsService} interface.
 * This service is responsible for looking up user details (by username) during authentication.
 * <p>
 * Integrates your application's persistent User data with the security system.
 * </p>
 */
@Service // Instructs Spring to detect, instantiate, and manage this class as a service bean
public class UserDetailsServiceImpl implements UserDetailsService {

    // Injects the UserRepository dependency (Spring will provide an implementation at runtime)
    @Autowired
    UserRepository userRepository;

    /**
     * Locates the user in the database by their username for authentication purposes.
     * <p>
     * Required by the {@link UserDetailsService} interface, this is called automatically by Spring Security
     * when a login attempt occurs. If a username is not found, a specific exception is thrown,
     * which will result in authentication failure.
     * </p>
     * <p>
     * Annotated with @Transactional to ensure the read operation participates in a transaction (useful for lazy loading, etc).
     * </p>
     *
     * @param username the username identifying the user whose data is required
     * @return UserDetails used by Spring Security (custom implementation)
     * @throws UsernameNotFoundException if the user could not be found in the repository
     */
    @Override
    @Transactional // Ensures the operation runs with transaction support (safer DB access)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find user by username first, then by email
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail)) // Try email if username not found
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username or email: " + usernameOrEmail));

        // Build a Spring Security-compatible UserDetails object
        return UserDetailsImpl.build(user);
    }

}