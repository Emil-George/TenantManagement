package com.nbjgroup.service;

import com.nbjgroup.dto.auth.RegisterRequest;
import com.nbjgroup.entity.User; // Correct import
import com.nbjgroup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerNewUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());

        // Encode the password before saving
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Set the role using the nested Role enum from the User class
        user.setRole(User.Role.TENANT); // Corrected: Uses User.Role

        // Set the active status using the correct method name
        user.setIsActive(true); // Corrected: setIsActive instead of setActive

        // Set other default fields that exist in your User entity
        user.setEmailVerified(true); // This field exists
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}
