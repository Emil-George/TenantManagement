package com.nbjgroup.security;

import com.nbjgroup.entity.User;
import com.nbjgroup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for loading user-specific data.
 * Integrates with Spring Security authentication mechanism.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Load user by username (email in our case)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        //Check if the user is active ---
        if (!user.getIsActive()) {
            logger.warn("Authentication attempt for inactive user: {}", email);
            throw new DisabledException("User account is inactive.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
    /**
     * Load user by ID (useful for JWT token validation)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        logger.debug("Loading user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new UsernameNotFoundException("User not found with ID: " + id);
                });

        logger.debug("User found: {} with role: {}", user.getEmail(), user.getRole());
        
        // Check if user account is active
        if (!user.getIsActive()) {
            logger.warn("User account is inactive: {}", user.getEmail());
            throw new UsernameNotFoundException("User account is inactive: " + user.getEmail());
        }

        return user;
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get user entity by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Get user entity by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) throws UsernameNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
    }

    /**
     * Validate user credentials and account status
     */
    public boolean validateUserAccount(String email) {
        try {
            User user = getUserByEmail(email);
            
            // Check if account is active
            if (!user.getIsActive()) {
                logger.warn("Account validation failed - inactive account: {}", email);
                return false;
            }
            
            // Check if email is verified (optional check)
            if (!user.getEmailVerified()) {
                logger.warn("Account validation failed - email not verified: {}", email);
                // You might want to return false here if email verification is mandatory
                // return false;
            }
            
            return true;
        } catch (UsernameNotFoundException e) {
            logger.warn("Account validation failed - user not found: {}", email);
            return false;
        }
    }

    /**
     * Update user last login timestamp
     */
    @Transactional
    public void updateLastLogin(String email) {
        try {
            User user = getUserByEmail(email);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            userRepository.save(user);
            logger.debug("Updated last login for user: {}", email);
        } catch (Exception e) {
            logger.error("Failed to update last login for user: {}", email, e);
        }
    }

    /**
     * Activate user account
     */
    @Transactional
    public void activateUser(String email) {
        try {
            User user = getUserByEmail(email);
            user.setIsActive(true);
            user.setEmailVerified(true);
            userRepository.save(user);
            logger.info("Activated user account: {}", email);
        } catch (Exception e) {
            logger.error("Failed to activate user account: {}", email, e);
            throw new RuntimeException("Failed to activate user account", e);
        }
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateUser(String email) {
        try {
            User user = getUserByEmail(email);
            user.setIsActive(false);
            userRepository.save(user);
            logger.info("Deactivated user account: {}", email);
        } catch (Exception e) {
            logger.error("Failed to deactivate user account: {}", email, e);
            throw new RuntimeException("Failed to deactivate user account", e);
        }
    }

    /**
     * Verify user email
     */
    @Transactional
    public void verifyUserEmail(String email) {
        try {
            User user = getUserByEmail(email);
            user.setEmailVerified(true);
            userRepository.save(user);
            logger.info("Verified email for user: {}", email);
        } catch (Exception e) {
            logger.error("Failed to verify email for user: {}", email, e);
            throw new RuntimeException("Failed to verify user email", e);
        }
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String email, User.Role role) {
        try {
            User user = getUserByEmail(email);
            return user.getRole() == role;
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(String email) {
        return hasRole(email, User.Role.ADMIN);
    }

    /**
     * Check if user is tenant
     */
    public boolean isTenant(String email) {
        return hasRole(email, User.Role.TENANT);
    }
}
