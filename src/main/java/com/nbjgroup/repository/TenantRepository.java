package com.nbjgroup.repository;

import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Tenant entity operations.
 * Provides data access methods for tenant management and property operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Find tenant by user ID
     */
    Optional<Tenant> findByUserId(Long userId);
    Optional<Tenant> findByUser(User user);

    /**
     * Find tenant by user email
     */
    @Query("SELECT t FROM Tenant t JOIN t.user u WHERE u.email = :email")
    Optional<Tenant> findByUserEmail(@Param("email") String email);

    /**
     * Find tenants by status
     */
    List<Tenant> findByStatus(Tenant.TenantStatus status);

    /**
     * Find tenants by status with pagination
     */
    Page<Tenant> findByStatus(Tenant.TenantStatus status, Pageable pageable);

    /**
     * Find active tenants
     */
    List<Tenant> findByStatusOrderByCreatedAtDesc(Tenant.TenantStatus status);

    /**
     * Find tenants by property address
     */
    List<Tenant> findByPropertyAddressContainingIgnoreCase(String propertyAddress);

    /**
     * Find tenants by property address and unit number
     */
    Optional<Tenant> findByPropertyAddressAndUnitNumber(String propertyAddress, String unitNumber);

    /**
     * Find tenants with lease expiring soon
     */
    @Query("SELECT t FROM Tenant t WHERE t.leaseEndDate <= :expirationDate AND t.status = :status")
    List<Tenant> findTenantsWithExpiringLeases(@Param("expirationDate") LocalDate expirationDate, 
                                              @Param("status") Tenant.TenantStatus status);

    /**
     * Find tenants with expired leases
     */
    @Query("SELECT t FROM Tenant t WHERE t.leaseEndDate < :currentDate AND t.status = :status")
    List<Tenant> findTenantsWithExpiredLeases(@Param("currentDate") LocalDate currentDate, 
                                             @Param("status") Tenant.TenantStatus status);

    /**
     * Find tenants by rent amount range
     */
    @Query("SELECT t FROM Tenant t WHERE t.rentAmount BETWEEN :minRent AND :maxRent")
    List<Tenant> findByRentAmountBetween(@Param("minRent") BigDecimal minRent, 
                                        @Param("maxRent") BigDecimal maxRent);

    /**
     * Search tenants by name or property
     */
    @Query("SELECT t FROM Tenant t JOIN t.user u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.unitNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Tenant> searchTenants(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find tenants with pending maintenance requests
     */
    @Query("SELECT DISTINCT t FROM Tenant t JOIN t.maintenanceRequests mr WHERE " +
           "mr.status IN ('PENDING', 'IN_PROGRESS', 'ASSIGNED')")
    List<Tenant> findTenantsWithPendingMaintenance();

    /**
     * Find tenants with overdue payments
     */
    @Query("SELECT DISTINCT t FROM Tenant t JOIN t.payments p WHERE " +
           "p.status = 'PENDING' AND p.dueDate < :currentDate")
    List<Tenant> findTenantsWithOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Find tenants by move-in date range
     */
    @Query("SELECT t FROM Tenant t WHERE t.moveInDate BETWEEN :startDate AND :endDate")
    List<Tenant> findByMoveInDateBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Count tenants by status
     */
    long countByStatus(Tenant.TenantStatus status);

    /**
     * Count tenants by property address
     */
    long countByPropertyAddress(String propertyAddress);

    /**
     * Get tenant statistics
     */
    @Query("SELECT " +
           "COUNT(t) as totalTenants, " +
           "SUM(CASE WHEN t.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeTenants, " +
           "SUM(CASE WHEN t.status = 'PENDING' THEN 1 ELSE 0 END) as pendingTenants, " +
           "AVG(t.rentAmount) as averageRent, " +
           "SUM(t.rentAmount) as totalRentAmount " +
           "FROM Tenant t")
    Object[] getTenantStatistics();

    /**
     * Find tenants with user details (for admin dashboard)
     */
    @Query("SELECT t FROM Tenant t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<Tenant> findAllWithUserDetails();

    /**
     * Find tenants with user details and pagination
     */
    @Query("SELECT t FROM Tenant t JOIN FETCH t.user")
    Page<Tenant> findAllWithUserDetails(Pageable pageable);

    /**
     * Find tenants by emergency contact information
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.emergencyContactName) LIKE LOWER(CONCAT('%', :contactName, '%')) OR " +
           "t.emergencyContactPhone = :contactPhone")
    List<Tenant> findByEmergencyContact(@Param("contactName") String contactName, 
                                       @Param("contactPhone") String contactPhone);

    /**
     * Find tenants with no lease agreements
     */
    @Query("SELECT t FROM Tenant t WHERE t.leaseAgreements IS EMPTY")
    List<Tenant> findTenantsWithoutLeaseAgreements();

    /**
     * Find tenants with active lease agreements
     */
    @Query("SELECT DISTINCT t FROM Tenant t JOIN t.leaseAgreements la WHERE la.status = 'ACTIVE'")
    List<Tenant> findTenantsWithActiveLeases();

    /**
     * Advanced search with multiple criteria
     */
    @Query("SELECT t FROM Tenant t JOIN t.user u WHERE " +
           "(:propertyAddress IS NULL OR LOWER(t.propertyAddress) LIKE LOWER(CONCAT('%', :propertyAddress, '%'))) AND " +
           "(:unitNumber IS NULL OR LOWER(t.unitNumber) LIKE LOWER(CONCAT('%', :unitNumber, '%'))) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:minRent IS NULL OR t.rentAmount >= :minRent) AND " +
           "(:maxRent IS NULL OR t.rentAmount <= :maxRent) AND " +
           "(:tenantName IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :tenantName, '%')))")
    Page<Tenant> findByMultipleCriteria(@Param("propertyAddress") String propertyAddress,
                                       @Param("unitNumber") String unitNumber,
                                       @Param("status") Tenant.TenantStatus status,
                                       @Param("minRent") BigDecimal minRent,
                                       @Param("maxRent") BigDecimal maxRent,
                                       @Param("tenantName") String tenantName,
                                       Pageable pageable);

    /**
     * Find tenants by lease date range
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "(:startDate IS NULL OR t.leaseStartDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.leaseEndDate <= :endDate)")
    List<Tenant> findByLeaseDateRange(@Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    /**
     * Get monthly rent collection summary
     */
    @Query("SELECT " +
           "SUM(t.rentAmount) as totalExpectedRent, " +
           "COUNT(t) as totalTenants, " +
           "AVG(t.rentAmount) as averageRent " +
           "FROM Tenant t WHERE t.status = 'ACTIVE'")
    Object[] getMonthlyRentSummary();
}
