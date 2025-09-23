package com.nbjgroup.repository;

import com.nbjgroup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides data access methods for user management and authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address (used for authentication)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find users by role
     */
    List<User> findByRole(User.Role role);

    /**
     * Find users by role with pagination
     */
    Page<User> findByRole(User.Role role, Pageable pageable);

    /**
     * Find active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find active users by role
     */
    List<User> findByRoleAndIsActiveTrue(User.Role role);

    /**
     * Find users by email verification status
     */
    List<User> findByEmailVerified(Boolean emailVerified);

    /**
     * Search users by name (first name or last name)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Search users by name with pagination
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find users created within a date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find recently registered users (within last N days)
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :sinceDate ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegistered(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count users by role
     */
    long countByRole(User.Role role);

    /**
     * Count active users
     */
    long countByIsActiveTrue();

    /**
     * Count active users by role
     */
    long countByRoleAndIsActiveTrue(User.Role role);

    /**
     * Count unverified users
     */
    long countByEmailVerifiedFalse();

    /**
     * Find users with tenant information (for admin dashboard)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.role = :role")
    List<User> findUsersWithTenantInfo(@Param("role") User.Role role);

    /**
     * Find users by phone number
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Find inactive users for cleanup
     */
    @Query("SELECT u FROM User u WHERE u.isActive = false AND u.updatedAt < :cutoffDate")
    List<User> findInactiveUsersBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Advanced search with multiple criteria
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findByMultipleCriteria(@Param("email") String email,
                                     @Param("firstName") String firstName,
                                     @Param("lastName") String lastName,
                                     @Param("role") User.Role role,
                                     @Param("isActive") Boolean isActive,
                                     Pageable pageable);

    /**
     * Get user statistics
     */
    @Query("SELECT " +
           "COUNT(u) as totalUsers, " +
           "SUM(CASE WHEN u.role = 'ADMIN' THEN 1 ELSE 0 END) as adminCount, " +
           "SUM(CASE WHEN u.role = 'TENANT' THEN 1 ELSE 0 END) as tenantCount, " +
           "SUM(CASE WHEN u.isActive = true THEN 1 ELSE 0 END) as activeCount, " +
           "SUM(CASE WHEN u.emailVerified = true THEN 1 ELSE 0 END) as verifiedCount " +
           "FROM User u")
    Object[] getUserStatistics();
}
