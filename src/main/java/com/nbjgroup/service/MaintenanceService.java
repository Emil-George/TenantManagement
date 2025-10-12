package com.nbjgroup.service;

import com.nbjgroup.dto.maintenance.MaintenanceRequestDTO;
import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.MaintenanceRequestRepository;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MaintenanceService {

    @Autowired
    private MaintenanceRequestRepository maintenanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional
    public MaintenanceRequest createMaintenanceRequest(MaintenanceRequestDTO requestDTO, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Tenant tenant = tenantRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("No tenant profile found for user. Please complete your profile."));

        MaintenanceRequest newRequest = new MaintenanceRequest();
        newRequest.setTitle(requestDTO.getTitle());
        newRequest.setDescription(requestDTO.getDescription());

        // --- THIS IS THE LINE TO FIX ---
        // Add .toUpperCase() to make it robust against case differences from the frontend.
        newRequest.setCategory(MaintenanceRequest.Category.valueOf(requestDTO.getCategory().toUpperCase()));

        newRequest.setStatus(MaintenanceRequest.RequestStatus.PENDING);
        newRequest.setPriority(MaintenanceRequest.Priority.valueOf(requestDTO.getPriority().toUpperCase()));
        newRequest.setCreatedAt(LocalDateTime.now());
        newRequest.setUpdatedAt(LocalDateTime.now());
        newRequest.setTenant(tenant);

        return maintenanceRepository.save(newRequest);
    }
}
