package com.nbjgroup.repository;

import com.nbjgroup.entity.Payment;
import com.nbjgroup.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Payment entity operations.
 * Provides data access methods for payment tracking and financial reporting.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>,JpaSpecificationExecutor<Payment>{

    /**
     * Find payments by tenant
     */
    List<Payment> findByTenantOrderByDueDateDesc(Tenant tenant);

    /**
     * Find payments by tenant with pagination
     */
    Page<Payment> findByTenantOrderByDueDateDesc(Tenant tenant, Pageable pageable);

    /**
     * Find payments by tenant ID
     */
    List<Payment> findByTenantIdOrderByDueDateDesc(Long tenantId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatusOrderByDueDateDesc(Payment.PaymentStatus status);

    /**
     * Find payments by status with pagination
     */
    Page<Payment> findByStatusOrderByDueDateDesc(Payment.PaymentStatus status, Pageable pageable);

    /**
     * Find overdue payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate < :currentDate")
    List<Payment> findOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Find payments due within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.dueDate BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsDueBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Find payments made within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsMadeBetween(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    /**
     * Find payments by payment type
     */
    List<Payment> findByPaymentTypeOrderByDueDateDesc(Payment.PaymentType paymentType);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethodOrderByPaymentDateDesc(Payment.PaymentMethod paymentMethod);

    /**
     * Find tenant's pending payments
     */
    @Query("SELECT p FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'PENDING'")
    List<Payment> findPendingPaymentsByTenant(@Param("tenantId") Long tenantId);

    /**
     * Find tenant's completed payments
     */
    @Query("SELECT p FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'COMPLETED' ORDER BY p.paymentDate DESC")
    List<Payment> findCompletedPaymentsByTenant(@Param("tenantId") Long tenantId);

    /**
     * Calculate total payments by tenant
     */
    @Query("SELECT SUM(p.totalAmount) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaymentsByTenant(@Param("tenantId") Long tenantId);

    /**
     * Calculate total outstanding amount by tenant
     */
    @Query("SELECT SUM(p.totalAmount) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'PENDING'")
    BigDecimal calculateOutstandingAmountByTenant(@Param("tenantId") Long tenantId);

    /**
     * Get monthly payment summary
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM p.paymentDate) as year, " +
           "EXTRACT(MONTH FROM p.paymentDate) as month, " +
           "SUM(p.totalAmount) as totalAmount, " +
           "COUNT(p) as paymentCount " +
           "FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(YEAR FROM p.paymentDate), EXTRACT(MONTH FROM p.paymentDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyPaymentSummary(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * Get payment statistics
     */
    @Query("SELECT " +
           "COUNT(p) as totalPayments, " +
           "SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedPayments, " +
           "SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END) as pendingPayments, " +
           "SUM(CASE WHEN p.status = 'PENDING' AND p.dueDate < CURRENT_DATE THEN 1 ELSE 0 END) as overduePayments, " +
           "SUM(CASE WHEN p.status = 'COMPLETED' THEN p.totalAmount ELSE 0 END) as totalCollected, " +
           "SUM(CASE WHEN p.status = 'PENDING' THEN p.totalAmount ELSE 0 END) as totalOutstanding " +
           "FROM Payment p")
    Object[] getPaymentStatistics();

    /**
     * Find payments with late fees
     */
    @Query("SELECT p FROM Payment p WHERE p.lateFee > 0 ORDER BY p.dueDate DESC")
    List<Payment> findPaymentsWithLateFees();

    /**
     * Calculate total late fees collected
     */
    @Query("SELECT SUM(p.lateFee) FROM Payment p WHERE p.status = 'COMPLETED' AND p.lateFee > 0")
    BigDecimal calculateTotalLateFeesCollected();

    /**
     * Find recent payments (last N days)
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate >= :sinceDate ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments(@Param("sinceDate") LocalDate sinceDate);

    /**
     * Search payments by tenant name or transaction ID
     */
    @Query("SELECT p FROM Payment p JOIN p.tenant t JOIN t.user u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Payment> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM Payment p WHERE p.totalAmount BETWEEN :minAmount AND :maxAmount")
    List<Payment> findByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                   @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Count overdue payments
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate < :currentDate")
    long countOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Count payments by status
     */
    long countByStatus(Payment.PaymentStatus status);

    /**
     * Find payments by tenant and date range
     */
    @Query("SELECT p FROM Payment p WHERE p.tenant.id = :tenantId AND p.dueDate BETWEEN :startDate AND :endDate")
    List<Payment> findByTenantAndDateRange(@Param("tenantId") Long tenantId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    /**
     * Get tenant payment history with details
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.tenant t JOIN FETCH t.user WHERE p.tenant.id = :tenantId ORDER BY p.dueDate DESC")
    List<Payment> findTenantPaymentHistory(@Param("tenantId") Long tenantId);

    /**
     * Advanced payment search with multiple criteria
     */
    @Query("SELECT p FROM Payment p JOIN p.tenant t JOIN t.user u WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:paymentType IS NULL OR p.paymentType = :paymentType) AND " +
           "(:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) AND " +
           "(:minAmount IS NULL OR p.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR p.totalAmount <= :maxAmount) AND " +
           "(:startDate IS NULL OR p.dueDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.dueDate <= :endDate) AND " +
           "(:tenantName IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :tenantName, '%')))")
    Page<Payment> findByMultipleCriteria(@Param("status") Payment.PaymentStatus status,
                                        @Param("paymentType") Payment.PaymentType paymentType,
                                        @Param("paymentMethod") Payment.PaymentMethod paymentMethod,
                                        @Param("minAmount") BigDecimal minAmount,
                                        @Param("maxAmount") BigDecimal maxAmount,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("tenantName") String tenantName,
                                        Pageable pageable);
}
