package com.nbjgroup.controller;

import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.MaintenanceRequestRepository;
import com.nbjgroup.repository.PaymentRepository; // Assuming you have this
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard" )
public class DashboardController {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private MaintenanceRequestRepository maintenanceRequestRepository;

    // Inject other repositories like PaymentRepository as needed

    @GetMapping("/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> getTenantDashboardData() {
        // Get the logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Find the tenant profile
        Tenant tenant = tenantRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Tenant profile not found for user: " + userEmail));

        // --- Aggregate Data ---
        // This is where you'll gather all the pieces of data for the dashboard.

        // 1. Profile Data (you can create a DTO for this)
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", tenant.getUser().getFirstName() + " " + tenant.getUser().getLastName());
        profileData.put("email", tenant.getUser().getEmail());
        profileData.put("propertyAddress", tenant.getPropertyAddress());
        profileData.put("leaseEnd", tenant.getLeaseEndDate());
        // ... add other profile fields

        // 2. Maintenance Data
        long activeMaintenance = maintenanceRequestRepository.countByTenantAndStatus(tenant, MaintenanceRequest.RequestStatus.IN_PROGRESS);
        long pendingMaintenance = maintenanceRequestRepository.countByTenantAndStatus(tenant, MaintenanceRequest.RequestStatus.PENDING);
        long completedMaintenance = maintenanceRequestRepository.countByTenantAndStatus(tenant, MaintenanceRequest.RequestStatus.COMPLETED);

        Map<String, Object> maintenanceData = new HashMap<>();
        maintenanceData.put("active", activeMaintenance);
        maintenanceData.put("pending", pendingMaintenance);
        maintenanceData.put("completed", completedMaintenance);

        // 3. Payment Data (Example - you'll need to implement this logic)
        Map<String, Object> paymentData = new HashMap<>();

// Create the 'nextDue' map separately to handle potential nulls
        Map<String, Object> nextDueData = new HashMap<>();
        nextDueData.put("amount", tenant.getRentAmount() != null ? tenant.getRentAmount() : 0.0); // Use 0.0 as a default if rent is null
        nextDueData.put("dueDate", "2024-11-01"); // Placeholder

        paymentData.put("nextDue", nextDueData);
        paymentData.put("balance", 0); // Placeholder

        // --- Build the Final Response ---
        Map<String, Object> dashboardResponse = new HashMap<>();
        dashboardResponse.put("profile", profileData);
        dashboardResponse.put("maintenance", maintenanceData);
        dashboardResponse.put("payments", paymentData);
        // ... add other sections like 'lease'

        return ResponseEntity.ok(dashboardResponse);
    }
}
