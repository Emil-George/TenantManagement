package com.nbjgroup.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MaintenanceRequest entity representing maintenance issues submitted by tenants.
 * Handles request lifecycle, priority management, and file attachments.
 */
@Entity
@Table(name = "maintenance_requests", indexes = {
    @Index(name = "idx_maintenance_tenant", columnList = "tenant_id"),
    @Index(name = "idx_maintenance_status", columnList = "status"),
    @Index(name = "idx_maintenance_priority", columnList = "priority"),
    @Index(name = "idx_maintenance_category", columnList = "category"),
    @Index(name = "idx_maintenance_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One relationship with Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference
    private Tenant tenant;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category = Category.OTHER;

    @Column(name = "location_details")
    private String locationDetails;

    @Column(name = "preferred_contact_method")
    private String preferredContactMethod;

    @Column(name = "preferred_time")
    private String preferredTime;

    @Column(name = "tenant_available")
    private Boolean tenantAvailable = true;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "tenant_feedback", columnDefinition = "TEXT")
    private String tenantFeedback;

    @Column(name = "tenant_rating")
    private Integer tenantRating; // 1-5 stars

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationship with MaintenanceRequestFile
    @OneToMany(mappedBy = "maintenanceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MaintenanceRequestFile> attachments = new ArrayList<>();

    // Constructors
    public MaintenanceRequest() {}

    public MaintenanceRequest(Tenant tenant, String title, String description, Category category, Priority priority) {
        this.tenant = tenant;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = RequestStatus.PENDING;
    }

    // Business methods
    public boolean isOverdue() {
        if (scheduledDate == null || isCompleted()) return false;
        return LocalDateTime.now().isAfter(scheduledDate);
    }

    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == RequestStatus.IN_PROGRESS;
    }

    public boolean canBeAssigned() {
        return status == RequestStatus.PENDING || status == RequestStatus.APPROVED;
    }

    public boolean canBeStarted() {
        return status == RequestStatus.ASSIGNED && assignedTo != null;
    }

    public boolean canBeCompleted() {
        return status == RequestStatus.IN_PROGRESS;
    }

    public long getHoursSinceCreated() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    public long getHoursToComplete() {
        if (completedAt == null || startedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).toHours();
    }

    public void assignTo(String assignee) {
        this.assignedTo = assignee;
        this.assignedAt = LocalDateTime.now();
        this.status = RequestStatus.ASSIGNED;
    }

    public void start() {
        if (canBeStarted()) {
            this.status = RequestStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete(String resolutionSummary, BigDecimal actualCost) {
        if (canBeCompleted()) {
            this.status = RequestStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
            this.resolutionSummary = resolutionSummary;
            this.actualCost = actualCost;
        }
    }

    public void cancel(String reason) {
        this.status = RequestStatus.CANCELLED;
        this.adminNotes = (adminNotes != null ? adminNotes + "\n" : "") + "Cancelled: " + reason;
    }

    public void addFeedback(String feedback, Integer rating) {
        this.tenantFeedback = feedback;
        this.tenantRating = rating;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public Boolean getTenantAvailable() {
        return tenantAvailable;
    }

    public void setTenantAvailable(Boolean tenantAvailable) {
        this.tenantAvailable = tenantAvailable;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getTenantFeedback() {
        return tenantFeedback;
    }

    public void setTenantFeedback(String tenantFeedback) {
        this.tenantFeedback = tenantFeedback;
    }

    public Integer getTenantRating() {
        return tenantRating;
    }

    public void setTenantRating(Integer tenantRating) {
        this.tenantRating = tenantRating;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
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

    public List<MaintenanceRequestFile> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MaintenanceRequestFile> attachments) {
        this.attachments = attachments;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceRequest that = (MaintenanceRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MaintenanceRequest{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", category=" + category +
                '}';
    }

    /**
     * Enum representing maintenance request status
     */
    public enum RequestStatus {
        PENDING("Pending Review"),
        APPROVED("Approved"),
        ASSIGNED("Assigned"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled"),
        ON_HOLD("On Hold");

        private final String displayName;

        RequestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum representing maintenance request priority
     */
    public enum Priority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        URGENT("Urgent"),
        EMERGENCY("Emergency");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum representing maintenance request categories
     */
    public enum Category {
        PLUMBING("Plumbing"),
        ELECTRICAL("Electrical"),
        HVAC("HVAC"),
        APPLIANCES("Appliances"),
        FLOORING("Flooring"),
        PAINTING("Painting"),
        DOORS_WINDOWS("Doors & Windows"),
        SECURITY("Security"),
        PEST_CONTROL("Pest Control"),
        CLEANING("Cleaning"),
        LANDSCAPING("Landscaping"),
        OTHER("Other");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
