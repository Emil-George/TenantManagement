package com.nbjgroup.repository;

import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    // Methods for the controller's filtering
    Page<MaintenanceRequest> findByTenant(Tenant tenant, Pageable pageable);
    Page<MaintenanceRequest> findByStatus(MaintenanceRequest.RequestStatus status, Pageable pageable);
    Page<MaintenanceRequest> findByPriorityOrderByCreatedAtDesc(MaintenanceRequest.Priority priority, Pageable pageable);
    Page<MaintenanceRequest> findByStatusAndPriority(MaintenanceRequest.RequestStatus status, MaintenanceRequest.Priority priority, Pageable pageable);

    // Existing and other useful methods
    List<MaintenanceRequest> findByTenantOrderByCreatedAtDesc(Tenant tenant);
    Page<MaintenanceRequest> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable);
    List<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<MaintenanceRequest> findByStatusOrderByCreatedAtDesc(MaintenanceRequest.RequestStatus status);
    List<MaintenanceRequest> findByPriorityOrderByCreatedAtDesc(MaintenanceRequest.Priority priority);
    List<MaintenanceRequest> findByCategoryOrderByCreatedAtDesc(String category); // Changed to String

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.status IN ('PENDING', 'APPROVED') ORDER BY mr.priority DESC, mr.createdAt ASC")
    List<MaintenanceRequest> findPendingRequests();

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.status IN ('ASSIGNED', 'IN_PROGRESS') ORDER BY mr.scheduledDate ASC")
    List<MaintenanceRequest> findActiveRequests();

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.scheduledDate < :currentDateTime AND mr.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<MaintenanceRequest> findOverdueRequests(@Param("currentDateTime") LocalDateTime currentDateTime);

    List<MaintenanceRequest> findByAssignedToOrderByScheduledDateAsc(String assignedTo);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.createdAt BETWEEN :startDate AND :endDate ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.completedAt BETWEEN :startDate AND :endDate ORDER BY mr.completedAt DESC")
    List<MaintenanceRequest> findByCompletedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.priority IN ('HIGH', 'URGENT', 'EMERGENCY') AND mr.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY mr.priority DESC, mr.createdAt ASC")
    List<MaintenanceRequest> findHighPriorityRequests();

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE " +
            "LOWER(mr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(mr.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<MaintenanceRequest> searchRequests(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT DISTINCT mr FROM MaintenanceRequest mr JOIN mr.attachments WHERE mr.attachments IS NOT EMPTY")
    List<MaintenanceRequest> findRequestsWithAttachments();

    long countByStatus(MaintenanceRequest.RequestStatus status);
    long countByPriority(MaintenanceRequest.Priority priority);
    long countByCategory(String category); // Changed to String

    @Query("SELECT COUNT(mr) FROM MaintenanceRequest mr WHERE mr.scheduledDate < :currentDateTime AND mr.status NOT IN ('COMPLETED', 'CANCELLED')")
    long countOverdueRequests(@Param("currentDateTime") LocalDateTime currentDateTime);

    @Query(value = "SELECT " +
            "COUNT(*) as totalRequests, " +
            "SUM(CASE WHEN mr.status = 'PENDING' THEN 1 ELSE 0 END) as pendingRequests, " +
            "SUM(CASE WHEN mr.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgressRequests, " +
            "SUM(CASE WHEN mr.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedRequests, " +
            "SUM(CASE WHEN mr.priority IN ('HIGH', 'URGENT', 'EMERGENCY') THEN 1 ELSE 0 END) as highPriorityRequests, " +
            "AVG(mr.rating) as averageRating " +
            "FROM maintenance_requests mr", nativeQuery = true)
    Object[] getMaintenanceStatistics();

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.createdAt >= :sinceDate ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findRecentRequests(@Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.tenant.id = :tenantId AND mr.status = :status ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") MaintenanceRequest.RequestStatus status);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.scheduledDate BETWEEN :startDate AND :endDate ORDER BY mr.scheduledDate ASC")
    List<MaintenanceRequest> findScheduledBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.tenantFeedback IS NOT NULL OR mr.tenantRating IS NOT NULL ORDER BY mr.completedAt DESC")
    List<MaintenanceRequest> findRequestsWithFeedback();

    @Query("SELECT AVG(CAST(TIMESTAMPDIFF(SECOND, mr.startedAt, mr.completedAt) AS DOUBLE) / 3600.0) FROM MaintenanceRequest mr WHERE mr.startedAt IS NOT NULL AND mr.completedAt IS NOT NULL")
    Double calculateAverageCompletionTimeInHours();

    @Query("SELECT mr FROM MaintenanceRequest mr JOIN mr.tenant t WHERE LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :propertyAddress, '%')) ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findByPropertyAddress(@Param("propertyAddress") String propertyAddress);

    @Query("SELECT mr FROM MaintenanceRequest mr JOIN mr.tenant t JOIN t.user u WHERE " +
            "(:status IS NULL OR mr.status = :status) AND " +
            "(:priority IS NULL OR mr.priority = :priority) AND " +
            "(:category IS NULL OR mr.category = :category) AND " +
            "(:assignedTo IS NULL OR LOWER(mr.assignedTo) LIKE LOWER(CONCAT('%', :assignedTo, '%'))) AND " +
            "(:tenantName IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :tenantName, '%'))) AND " +
            "(:propertyAddress IS NULL OR LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :propertyAddress, '%')))")
    Page<MaintenanceRequest> findByMultipleCriteria(@Param("status") MaintenanceRequest.RequestStatus status,
                                                    @Param("priority") MaintenanceRequest.Priority priority,
                                                    @Param("category") String category, // Changed to String
                                                    @Param("assignedTo") String assignedTo,
                                                    @Param("tenantName") String tenantName,
                                                    @Param("propertyAddress") String propertyAddress,
                                                    Pageable pageable);

    @Query(value = "SELECT " +
            "EXTRACT(YEAR FROM mr.created_at) as year, " +
            "EXTRACT(MONTH FROM mr.created_at) as month, " +
            "COUNT(*) as requestCount, " +
            "SUM(CASE WHEN mr.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
            "AVG(mr.rating) as averageRating " +
            "FROM maintenance_requests mr WHERE mr.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY EXTRACT(YEAR FROM mr.created_at), EXTRACT(MONTH FROM mr.created_at) " +
            "ORDER BY year DESC, month DESC", nativeQuery = true)
    List<Object[]> getMonthlyMaintenanceSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
