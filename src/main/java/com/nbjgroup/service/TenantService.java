package com.nbjgroup.service;

import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a Tenant profile and links it to a User.
     * This is a crucial piece of business logic.
     */
    @Transactional
    public Tenant createTenantForUser(User user, String propertyAddress, String emergencyContactPhone) {
        if (tenantRepository.findByUser(user).isPresent()) {
            throw new IllegalStateException("User already has a tenant profile.");
        }

        Tenant tenant = new Tenant();
        tenant.setUser(user);
        tenant.setPropertyAddress(propertyAddress);
        tenant.setEmergencyContactPhone(emergencyContactPhone);
        // Set other defaults if necessary
        tenant.setLeaseStartDate(null); // Will be set when a lease is created
        tenant.setLeaseEndDate(null);

        return tenantRepository.save(tenant);
    }
}
