package com.nbjgroup.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LeaseAgreement entity representing lease contracts between tenants and property management.
 * Handles lease document storage, renewal tracking, and lease lifecycle management.
 */
@Entity
@Table(name = "lease_agreements", indexes = {
    @Index(name = "idx_lease_tenant", columnList = "tenant_id"),
    @Index(name = "idx_lease_status", columnList = "status"),
    @Index(name = "idx_lease_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_lease_renewal", columnList = "is_renewal")
})
@EntityListeners(AuditingEntityListener.class)
public class LeaseAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One relationship with Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Monthly rent is required")
    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaseStatus status = LeaseStatus.DRAFT;

    @Column(name = "lease_document_path")
    private String leaseDocumentPath;

    @Column(name = "lease_document_name")
    private String leaseDocumentName;

    @Column(name = "signed_document_path")
    private String signedDocumentPath;

    @Column(name = "signed_document_name")
    private String signedDocumentName;

    @Column(name = "is_renewal", nullable = false)
    private Boolean isRenewal = false;

    @Column(name = "previous_lease_id")
    private Long previousLeaseId;

    @Column(name = "renewal_notice_sent")
    private Boolean renewalNoticeSent = false;

    @Column(name = "renewal_notice_date")
    private LocalDate renewalNoticeDate;

    @Column(name = "tenant_signed_date")
    private LocalDate tenantSignedDate;

    @Column(name = "admin_signed_date")
    private LocalDate adminSignedDate;

    @Column(name = "lease_terms", columnDefinition = "TEXT")
    private String leaseTerms;

    @Column(name = "special_conditions", columnDefinition = "TEXT")
    private String specialConditions;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public LeaseAgreement() {}

    public LeaseAgreement(Tenant tenant, LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent) {
        this.tenant = tenant;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.status = LeaseStatus.DRAFT;
        this.isRenewal = false;
    }

    // Business methods
    public boolean isActive() {
        return status == LeaseStatus.ACTIVE && 
               LocalDate.now().isAfter(startDate.minusDays(1)) && 
               LocalDate.now().isBefore(endDate.plusDays(1));
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return endDate.isBefore(thresholdDate) || endDate.equals(thresholdDate);
    }

    public long getDaysUntilExpiration() {
        return LocalDate.now().until(endDate).getDays();
    }

    public long getLeaseDurationInMonths() {
        return startDate.until(endDate).toTotalMonths();
    }

    public boolean isFullySigned() {
        return tenantSignedDate != null && adminSignedDate != null;
    }

    public boolean canBeActivated() {
        return status == LeaseStatus.SIGNED && isFullySigned() && !isExpired();
    }

    public void markAsRenewal(Long previousLeaseId) {
        this.isRenewal = true;
        this.previousLeaseId = previousLeaseId;
    }

    public void sendRenewalNotice() {
        this.renewalNoticeSent = true;
        this.renewalNoticeDate = LocalDate.now();
    }

    public void signByTenant() {
        this.tenantSignedDate = LocalDate.now();
        if (adminSignedDate != null) {
            this.status = LeaseStatus.SIGNED;
        }
    }

    public void signByAdmin() {
        this.adminSignedDate = LocalDate.now();
        if (tenantSignedDate != null) {
            this.status = LeaseStatus.SIGNED;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public LeaseStatus getStatus() {
        return status;
    }

    public void setStatus(LeaseStatus status) {
        this.status = status;
    }

    public String getLeaseDocumentPath() {
        return leaseDocumentPath;
    }

    public void setLeaseDocumentPath(String leaseDocumentPath) {
        this.leaseDocumentPath = leaseDocumentPath;
    }

    public String getLeaseDocumentName() {
        return leaseDocumentName;
    }

    public void setLeaseDocumentName(String leaseDocumentName) {
        this.leaseDocumentName = leaseDocumentName;
    }

    public String getSignedDocumentPath() {
        return signedDocumentPath;
    }

    public void setSignedDocumentPath(String signedDocumentPath) {
        this.signedDocumentPath = signedDocumentPath;
    }

    public String getSignedDocumentName() {
        return signedDocumentName;
    }

    public void setSignedDocumentName(String signedDocumentName) {
        this.signedDocumentName = signedDocumentName;
    }

    public Boolean getIsRenewal() {
        return isRenewal;
    }

    public void setIsRenewal(Boolean isRenewal) {
        this.isRenewal = isRenewal;
    }

    public Long getPreviousLeaseId() {
        return previousLeaseId;
    }

    public void setPreviousLeaseId(Long previousLeaseId) {
        this.previousLeaseId = previousLeaseId;
    }

    public Boolean getRenewalNoticeSent() {
        return renewalNoticeSent;
    }

    public void setRenewalNoticeSent(Boolean renewalNoticeSent) {
        this.renewalNoticeSent = renewalNoticeSent;
    }

    public LocalDate getRenewalNoticeDate() {
        return renewalNoticeDate;
    }

    public void setRenewalNoticeDate(LocalDate renewalNoticeDate) {
        this.renewalNoticeDate = renewalNoticeDate;
    }

    public LocalDate getTenantSignedDate() {
        return tenantSignedDate;
    }

    public void setTenantSignedDate(LocalDate tenantSignedDate) {
        this.tenantSignedDate = tenantSignedDate;
    }

    public LocalDate getAdminSignedDate() {
        return adminSignedDate;
    }

    public void setAdminSignedDate(LocalDate adminSignedDate) {
        this.adminSignedDate = adminSignedDate;
    }

    public String getLeaseTerms() {
        return leaseTerms;
    }

    public void setLeaseTerms(String leaseTerms) {
        this.leaseTerms = leaseTerms;
    }

    public String getSpecialConditions() {
        return specialConditions;
    }

    public void setSpecialConditions(String specialConditions) {
        this.specialConditions = specialConditions;
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

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaseAgreement that = (LeaseAgreement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LeaseAgreement{" +
                "id=" + id +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", monthlyRent=" + monthlyRent +
                ", status=" + status +
                ", isRenewal=" + isRenewal +
                '}';
    }

    /**
     * Enum representing lease agreement status
     */
    public enum LeaseStatus {
        DRAFT("Draft"),
        PENDING_SIGNATURE("Pending Signature"),
        SIGNED("Signed"),
        ACTIVE("Active"),
        EXPIRED("Expired"),
        TERMINATED("Terminated"),
        CANCELLED("Cancelled");

        private final String displayName;

        LeaseStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
