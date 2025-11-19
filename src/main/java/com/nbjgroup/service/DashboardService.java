package com.nbjgroup.service;

import com.nbjgroup.dto.dashboard.AdminDashboardDTO;
import com.nbjgroup.dto.dashboard.TenantDashboardDTO;
import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.MaintenanceRequestRepository;
import com.nbjgroup.repository.PropertyRepository;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRepository;
    @Autowired private PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboardData() {
                AdminDashboardDTO dashboardData = new AdminDashboardDTO();
                dashboardData.setTotalTenants(tenantRepository.count());
                dashboardData.setPendingMaintenanceRequests(maintenanceRepository.countByStatus(MaintenanceRequest.RequestStatus.PENDING));
                dashboardData.setTotalProperties(propertyRepository.count());
                return dashboardData;
    }

    @Transactional(readOnly = true)
    public TenantDashboardDTO getTenantDashboardData(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Tenant tenant = tenantRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Tenant profile not found"));

        TenantDashboardDTO dashboardData = new TenantDashboardDTO();

        // 1. Populate Profile Data
        TenantDashboardDTO.ProfileData profileData = new TenantDashboardDTO.ProfileData();
        profileData.name = user.getFirstName() + " " + user.getLastName();
        profileData.email = user.getEmail();
        profileData.propertyAddress = tenant.getPropertyAddress();
        dashboardData.setProfile(profileData);

        // 2. Populate Maintenance Data
        TenantDashboardDTO.MaintenanceData maintenanceData = new TenantDashboardDTO.MaintenanceData();
        List<MaintenanceRequest> allRequests = maintenanceRepository.findByTenantOrderByCreatedAtDesc(tenant);

        maintenanceData.pendingRequests = allRequests.stream().filter(r -> r.getStatus() == MaintenanceRequest.RequestStatus.PENDING).count();
        maintenanceData.activeRequests = allRequests.stream().filter(r -> r.getStatus() == MaintenanceRequest.RequestStatus.IN_PROGRESS).count();
        maintenanceData.completedRequests = allRequests.stream().filter(r -> r.getStatus() == MaintenanceRequest.RequestStatus.COMPLETED).count();
        // Get the 5 most recent requests
        maintenanceData.recentRequests = allRequests.stream().limit(5).toList();
        dashboardData.setMaintenance(maintenanceData);

        return dashboardData;
    }
}
