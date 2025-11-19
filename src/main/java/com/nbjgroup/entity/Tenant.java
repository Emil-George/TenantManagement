package com.nbjgroup.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tenant entity representing tenant-specific information and relationships.
 * Contains detailed tenant data, lease information, and relationships to payments and maintenance requests.
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_property_unit", columnList = "property_address, unit_number"),
    @Index(name = "idx_tenant_status", columnList = "status"),
    @Index(name = "idx_tenant_lease_dates", columnList = "lease_start_date, lease_end_date")
})
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-One relationship with User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonManagedReference
    private User user;

    @NotBlank(message = "Property address is required")
    @Column(name = "property_address", nullable = false)
    private String propertyAddress;

    @Column(name = "unit_number")
    private String unitNumber;

    @Column(name = "rent_amount", precision = 10, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship")
    private String emergencyContactRelationship;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationships
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("tenant-lease")
    private List<LeaseAgreement> leaseAgreements = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("tenant-payment")
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MaintenanceRequest> maintenanceRequests = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id") // column in tenants table
    private Property property;

    // getter + setter
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    // Constructors
    public Tenant() {}

    public Tenant(User user, String propertyAddress, String unitNumber, BigDecimal rentAmount) {
        this.user = user;
        this.propertyAddress = propertyAddress;
        this.unitNumber = unitNumber;
        this.rentAmount = rentAmount;
        this.status = TenantStatus.ACTIVE;
    }

    // Business methods
    public String getFullAddress() {
        return unitNumber != null && !unitNumber.trim().isEmpty() 
            ? propertyAddress + ", Unit " + unitNumber 
            : propertyAddress;
    }

    public boolean isActiveLeaseExists() {
        return leaseAgreements.stream()
            .anyMatch(lease -> lease.getStatus() == LeaseAgreement.LeaseStatus.ACTIVE);
    }

    public LeaseAgreement getCurrentLease() {
        return leaseAgreements.stream()
            .filter(lease -> lease.getStatus() == LeaseAgreement.LeaseStatus.ACTIVE)
            .findFirst()
            .orElse(null);
    }

    public BigDecimal getTotalPaid() {
        return payments.stream()
            .filter(payment -> payment.getStatus() == Payment.PaymentStatus.COMPLETED)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getPendingMaintenanceRequests() {
        return maintenanceRequests.stream()
            .filter(request -> request.getStatus() == MaintenanceRequest.RequestStatus.PENDING ||
                             request.getStatus() == MaintenanceRequest.RequestStatus.IN_PROGRESS)
            .count();
    }

    public boolean isLeaseExpiringSoon(int daysThreshold) {
        if (leaseEndDate == null) return false;
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return leaseEndDate.isBefore(thresholdDate) || leaseEndDate.equals(thresholdDate);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPropertyAddress() {
        return propertyAddress;
    }

    public void setPropertyAddress(String propertyAddress) {
        this.propertyAddress = propertyAddress;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(BigDecimal rentAmount) {
        this.rentAmount = rentAmount;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public LocalDate getLeaseStartDate() {
        return leaseStartDate;
    }

    public void setLeaseStartDate(LocalDate leaseStartDate) {
        this.leaseStartDate = leaseStartDate;
    }

    public LocalDate getLeaseEndDate() {
        return leaseEndDate;
    }

    public void setLeaseEndDate(LocalDate leaseEndDate) {
        this.leaseEndDate = leaseEndDate;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    public void setEmergencyContactRelationship(String emergencyContactRelationship) {
        this.emergencyContactRelationship = emergencyContactRelationship;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(LocalDate moveInDate) {
        this.moveInDate = moveInDate;
    }

    public LocalDate getMoveOutDate() {
        return moveOutDate;
    }

    public void setMoveOutDate(LocalDate moveOutDate) {
        this.moveOutDate = moveOutDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<LeaseAgreement> getLeaseAgreements() {
        return leaseAgreements;
    }

    public void setLeaseAgreements(List<LeaseAgreement> leaseAgreements) {
        this.leaseAgreements = leaseAgreements;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<MaintenanceRequest> getMaintenanceRequests() {
        return maintenanceRequests;
    }

    public void setMaintenanceRequests(List<MaintenanceRequest> maintenanceRequests) {
        this.maintenanceRequests = maintenanceRequests;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(id, tenant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", propertyAddress='" + propertyAddress + '\'' +
                ", unitNumber='" + unitNumber + '\'' +
                ", rentAmount=" + rentAmount +
                ", status=" + status +
                '}';
    }

    /**
     * Enum representing tenant status in the system
     */
    public enum TenantStatus {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        PENDING("Pending Approval"),
        TERMINATED("Terminated"),
        SUSPENDED("Suspended");

        private final String displayName;

        TenantStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
