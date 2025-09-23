package com.nbjgroup.repository;

import com.nbjgroup.entity.LeaseAgreement;
import com.nbjgroup.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LeaseAgreement entity operations.
 * Provides data access methods for lease management and renewal tracking.
 */
@Repository
public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, Long> {

    /**
     * Find lease agreements by tenant
     */
    List<LeaseAgreement> findByTenantOrderByStartDateDesc(Tenant tenant);

    /**
     * Find lease agreements by tenant with pagination
     */
    Page<LeaseAgreement> findByTenantOrderByStartDateDesc(Tenant tenant, Pageable pageable);

    /**
     * Find lease agreements by tenant ID
     */
    List<LeaseAgreement> findByTenantIdOrderByStartDateDesc(Long tenantId);

    /**
     * Find active lease agreement by tenant
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.tenant = :tenant AND la.status = 'ACTIVE'")
    Optional<LeaseAgreement> findActiveLeaseBytenant(@Param("tenant") Tenant tenant);

    /**
     * Find active lease agreement by tenant ID
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.tenant.id = :tenantId AND la.status = 'ACTIVE'")
    Optional<LeaseAgreement> findActiveLeaseByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find lease agreements by status
     */
    List<LeaseAgreement> findByStatusOrderByStartDateDesc(LeaseAgreement.LeaseStatus status);

    /**
     * Find lease agreements by status with pagination
     */
    Page<LeaseAgreement> findByStatusOrderByStartDateDesc(LeaseAgreement.LeaseStatus status, Pageable pageable);

    /**
     * Find lease agreements expiring within date range
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.endDate BETWEEN :startDate AND :endDate AND la.status = 'ACTIVE' ORDER BY la.endDate ASC")
    List<LeaseAgreement> findExpiringLeases(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Find expired lease agreements
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.endDate < :currentDate AND la.status = 'ACTIVE'")
    List<LeaseAgreement> findExpiredLeases(@Param("currentDate") LocalDate currentDate);

    /**
     * Find lease agreements starting within date range
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.startDate BETWEEN :startDate AND :endDate ORDER BY la.startDate ASC")
    List<LeaseAgreement> findLeasesStartingBetween(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);

    /**
     * Find renewal lease agreements
     */
    List<LeaseAgreement> findByIsRenewalTrueOrderByStartDateDesc();

    /**
     * Find lease agreements pending signature
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.status = 'PENDING_SIGNATURE' ORDER BY la.createdAt ASC")
    List<LeaseAgreement> findPendingSignature();

    /**
     * Find fully signed lease agreements
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.tenantSignedDate IS NOT NULL AND la.adminSignedDate IS NOT NULL ORDER BY la.adminSignedDate DESC")
    List<LeaseAgreement> findFullySigned();

    /**
     * Find lease agreements signed by tenant only
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.tenantSignedDate IS NOT NULL AND la.adminSignedDate IS NULL ORDER BY la.tenantSignedDate DESC")
    List<LeaseAgreement> findSignedByTenantOnly();

    /**
     * Find lease agreements signed by admin only
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.adminSignedDate IS NOT NULL AND la.tenantSignedDate IS NULL ORDER BY la.adminSignedDate DESC")
    List<LeaseAgreement> findSignedByAdminOnly();

    /**
     * Find lease agreements with renewal notice sent
     */
    List<LeaseAgreement> findByRenewalNoticeSentTrueOrderByRenewalNoticeDateDesc();

    /**
     * Find lease agreements needing renewal notice
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.endDate <= :noticeDate AND la.renewalNoticeSent = false AND la.status = 'ACTIVE'")
    List<LeaseAgreement> findNeedingRenewalNotice(@Param("noticeDate") LocalDate noticeDate);

    /**
     * Count lease agreements by status
     */
    long countByStatus(LeaseAgreement.LeaseStatus status);

    /**
     * Count active lease agreements
     */
    @Query("SELECT COUNT(la) FROM LeaseAgreement la WHERE la.status = 'ACTIVE'")
    long countActiveLeases();

    /**
     * Count lease agreements expiring soon
     */
    @Query("SELECT COUNT(la) FROM LeaseAgreement la WHERE la.endDate <= :expirationDate AND la.status = 'ACTIVE'")
    long countExpiringSoon(@Param("expirationDate") LocalDate expirationDate);

    /**
     * Get lease agreement statistics
     */
    @Query("SELECT " +
           "COUNT(la) as totalLeases, " +
           "SUM(CASE WHEN la.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeLeases, " +
           "SUM(CASE WHEN la.status = 'PENDING_SIGNATURE' THEN 1 ELSE 0 END) as pendingSignature, " +
           "SUM(CASE WHEN la.status = 'SIGNED' THEN 1 ELSE 0 END) as signedLeases, " +
           "SUM(CASE WHEN la.isRenewal = true THEN 1 ELSE 0 END) as renewalLeases, " +
           "AVG(la.monthlyRent) as averageRent " +
           "FROM LeaseAgreement la")
    Object[] getLeaseStatistics();

    /**
     * Find lease agreements by date range
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE " +
           "(:startDate IS NULL OR la.startDate >= :startDate) AND " +
           "(:endDate IS NULL OR la.endDate <= :endDate) " +
           "ORDER BY la.startDate DESC")
    List<LeaseAgreement> findByDateRange(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Search lease agreements by tenant name
     */
    @Query("SELECT la FROM LeaseAgreement la JOIN la.tenant t JOIN t.user u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :tenantName, '%'))")
    Page<LeaseAgreement> searchByTenantName(@Param("tenantName") String tenantName, Pageable pageable);

    /**
     * Find lease agreements by property address
     */
    @Query("SELECT la FROM LeaseAgreement la JOIN la.tenant t WHERE " +
           "LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :propertyAddress, '%'))")
    List<LeaseAgreement> findByPropertyAddress(@Param("propertyAddress") String propertyAddress);

    /**
     * Find lease agreements with documents
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.leaseDocumentPath IS NOT NULL OR la.signedDocumentPath IS NOT NULL")
    List<LeaseAgreement> findWithDocuments();

    /**
     * Find lease agreements without documents
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.leaseDocumentPath IS NULL AND la.signedDocumentPath IS NULL")
    List<LeaseAgreement> findWithoutDocuments();

    /**
     * Find recent lease agreements (last N days)
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE la.createdAt >= :sinceDate ORDER BY la.createdAt DESC")
    List<LeaseAgreement> findRecentLeases(@Param("sinceDate") LocalDate sinceDate);

    /**
     * Find lease agreements by previous lease ID (renewals)
     */
    List<LeaseAgreement> findByPreviousLeaseId(Long previousLeaseId);

    /**
     * Advanced search with multiple criteria
     */
    @Query("SELECT la FROM LeaseAgreement la JOIN la.tenant t JOIN t.user u WHERE " +
           "(:status IS NULL OR la.status = :status) AND " +
           "(:isRenewal IS NULL OR la.isRenewal = :isRenewal) AND " +
           "(:tenantName IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :tenantName, '%'))) AND " +
           "(:propertyAddress IS NULL OR LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :propertyAddress, '%'))) AND " +
           "(:startDate IS NULL OR la.startDate >= :startDate) AND " +
           "(:endDate IS NULL OR la.endDate <= :endDate)")
    Page<LeaseAgreement> findByMultipleCriteria(@Param("status") LeaseAgreement.LeaseStatus status,
                                               @Param("isRenewal") Boolean isRenewal,
                                               @Param("tenantName") String tenantName,
                                               @Param("propertyAddress") String propertyAddress,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               Pageable pageable);

    /**
     * Get monthly lease summary
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM la.startDate) as year, " +
           "EXTRACT(MONTH FROM la.startDate) as month, " +
           "COUNT(la) as leaseCount, " +
           "SUM(la.monthlyRent) as totalRent, " +
           "SUM(CASE WHEN la.isRenewal = true THEN 1 ELSE 0 END) as renewalCount " +
           "FROM LeaseAgreement la WHERE la.startDate BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(YEAR FROM la.startDate), EXTRACT(MONTH FROM la.startDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyLeaseSummary(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    /**
     * Find lease agreements requiring action (expiring, pending signature, etc.)
     */
    @Query("SELECT la FROM LeaseAgreement la WHERE " +
           "(la.status = 'PENDING_SIGNATURE') OR " +
           "(la.status = 'ACTIVE' AND la.endDate <= :expirationThreshold AND la.renewalNoticeSent = false) OR " +
           "(la.tenantSignedDate IS NOT NULL AND la.adminSignedDate IS NULL) OR " +
           "(la.adminSignedDate IS NOT NULL AND la.tenantSignedDate IS NULL) " +
           "ORDER BY " +
           "CASE " +
           "WHEN la.status = 'PENDING_SIGNATURE' THEN 1 " +
           "WHEN la.endDate <= :expirationThreshold THEN 2 " +
           "ELSE 3 " +
           "END, la.endDate ASC")
    List<LeaseAgreement> findRequiringAction(@Param("expirationThreshold") LocalDate expirationThreshold);
}
