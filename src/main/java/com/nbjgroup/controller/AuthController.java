package com.nbjgroup.controller;

import com.nbjgroup.dto.auth.AuthResponse;
import com.nbjgroup.dto.auth.LoginRequest;
import com.nbjgroup.dto.auth.RefreshTokenRequest;
import com.nbjgroup.dto.auth.RegisterRequest;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import com.nbjgroup.security.CustomUserDetailsService;
import com.nbjgroup.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication operations.
 * Handles user login, registration, token refresh, and password management.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for user: {}", loginRequest.getEmail());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            // Generate tokens
            JwtTokenProvider.JwtTokenResponse tokenResponse = tokenProvider.createTokenResponse(loginRequest.getEmail());
            
            // Get user details
            User user = userDetailsService.getUserByEmail(loginRequest.getEmail());
            
            // Update last login
            userDetailsService.updateLastLogin(loginRequest.getEmail());

            // Create response
            AuthResponse authResponse = new AuthResponse(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn(),
                tokenResponse.getRefreshExpiresIn(),
                user
            );

            logger.info("Login successful for user: {}", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            logger.warn("Login failed - invalid credentials for user: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Invalid email or password", "INVALID_CREDENTIALS"));
        } catch (AuthenticationException e) {
            logger.warn("Login failed - authentication error for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Authentication failed", "AUTHENTICATION_FAILED"));
        } catch (Exception e) {
            logger.error("Login failed - unexpected error for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Login failed due to server error", "SERVER_ERROR"));
        }
    }

    /**
     * User registration endpoint (for tenants)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for user: {}", registerRequest.getEmail());

            // Validate request
            if (!registerRequest.isPasswordMatching()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Passwords do not match", "PASSWORD_MISMATCH"));
            }

            if (!registerRequest.isTermsAccepted()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Terms and conditions must be accepted", "TERMS_NOT_ACCEPTED"));
            }

            // Check if user already exists
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                logger.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Email address is already registered", "EMAIL_ALREADY_EXISTS"));
            }

            // Create user entity
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setRole(User.Role.TENANT);
            user.setIsActive(true);
            user.setEmailVerified(true); // Auto-verify for demo purposes

            // Save user
            User savedUser = userRepository.save(user);

            // Create tenant profile if property information is provided
            if (registerRequest.getPropertyAddress() != null && !registerRequest.getPropertyAddress().trim().isEmpty()) {
                Tenant tenant = new Tenant();
                tenant.setUser(savedUser);
                tenant.setPropertyAddress(registerRequest.getPropertyAddress());
                tenant.setUnitNumber(registerRequest.getUnitNumber());
                tenant.setStatus(Tenant.TenantStatus.PENDING);
                
                tenantRepository.save(tenant);
                logger.info("Tenant profile created for user: {}", registerRequest.getEmail());
            }

            // Generate tokens
            JwtTokenProvider.JwtTokenResponse tokenResponse = tokenProvider.createTokenResponse(savedUser.getEmail());

            // Create response
            AuthResponse authResponse = new AuthResponse(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn(),
                tokenResponse.getRefreshExpiresIn(),
                savedUser
            );

            logger.info("Registration successful for user: {}", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);

        } catch (Exception e) {
            logger.error("Registration failed for user: {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Registration failed due to server error", "SERVER_ERROR"));
        }
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        try {
            logger.debug("Token refresh attempt");

            String refreshToken = refreshRequest.getRefreshToken();
            
            // Validate refresh token
            if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
                logger.warn("Token refresh failed - invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid or expired refresh token", "INVALID_REFRESH_TOKEN"));
            }

            // Generate new access token
            String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
            String username = tokenProvider.getUsernameFromToken(refreshToken);

            // Get user details
            User user = userDetailsService.getUserByEmail(username);

            // Create response with new access token and same refresh token
            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(newAccessToken);
            authResponse.setRefreshToken(refreshToken);
            authResponse.setExpiresIn(tokenProvider.getTokenRemainingTime(newAccessToken) / 1000);
            authResponse.setRefreshExpiresIn(tokenProvider.getTokenRemainingTime(refreshToken) / 1000);
            authResponse.setUser(new AuthResponse.UserInfo(user));

            logger.debug("Token refresh successful for user: {}", username);
            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Token refresh failed", "REFRESH_FAILED"));
        }
    }

    /**
     * Logout endpoint (client-side token invalidation)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // For JWT stateless authentication, logout is handled client-side
        // by removing the token from storage
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check authentication status
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated", "NOT_AUTHENTICATED"));
            }

            User user = (User) authentication.getPrincipal();
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user);
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to get user information", "SERVER_ERROR"));
        }
    }

    /**
     * Validate token endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            boolean isValid = tokenProvider.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            if (isValid) {
                String username = tokenProvider.getUsernameFromToken(token);
                long remainingTime = tokenProvider.getTokenRemainingTime(token);
                response.put("username", username);
                response.put("remainingTime", remainingTime);
                response.put("shouldRefresh", tokenProvider.shouldRefreshToken(token));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Token validation failed", "VALIDATION_FAILED"));
        }
    }

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        return errorResponse;
    }
}
