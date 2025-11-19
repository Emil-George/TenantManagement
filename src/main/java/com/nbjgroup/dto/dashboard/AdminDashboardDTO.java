package com.nbjgroup.dto.dashboard;

public class AdminDashboardDTO {
    private long totalTenants;
    private long pendingMaintenanceRequests;
    private long totalProperties;
    // private long pendingPayments; // Placeholder for future implementation

    public AdminDashboardDTO() {}

    public long getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(long totalTenants) {
        this.totalTenants = totalTenants;
    }

    public long getPendingMaintenanceRequests() {
        return pendingMaintenanceRequests;
    }

    public void setPendingMaintenanceRequests(long pendingMaintenanceRequests) {
        this.pendingMaintenanceRequests = pendingMaintenanceRequests;
    }

    public long getTotalProperties() {
        return totalProperties;
    }

    public void setTotalProperties(long totalProperties) {
        this.totalProperties = totalProperties;
    }
}
