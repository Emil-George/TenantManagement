package com.nbjgroup.dto.dashboard;

import com.nbjgroup.entity.MaintenanceRequest;
import java.util.List;

// This DTO will hold all the data for the tenant's dashboard
public class TenantDashboardDTO {

    private ProfileData profile;
    private MaintenanceData maintenance;
    // We can add payments and lease data later

    // Inner class for Profile Data
    public static class ProfileData {
        public String name;
        public String email;
        public String propertyAddress;
    }

    // Inner class for Maintenance Data
    public static class MaintenanceData {
        public long activeRequests;
        public long pendingRequests;
        public long completedRequests;
        public List<MaintenanceRequest> recentRequests;
    }

    // Getters and Setters
    public ProfileData getProfile() { return profile; }
    public void setProfile(ProfileData profile) { this.profile = profile; }
    public MaintenanceData getMaintenance() { return maintenance; }
    public void setMaintenance(MaintenanceData maintenance) { this.maintenance = maintenance; }
}
