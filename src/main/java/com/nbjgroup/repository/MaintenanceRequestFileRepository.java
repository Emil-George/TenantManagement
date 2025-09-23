package com.nbjgroup.repository;

import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.MaintenanceRequestFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MaintenanceRequestFile entity.
 * Provides data access methods for file management operations.
 */
@Repository
public interface MaintenanceRequestFileRepository extends JpaRepository<MaintenanceRequestFile, Long> {

    /**
     * Find all files associated with a maintenance request
     */
    List<MaintenanceRequestFile> findByMaintenanceRequest(MaintenanceRequest maintenanceRequest);

    /**
     * Find all files associated with a maintenance request ID
     */
    List<MaintenanceRequestFile> findByMaintenanceRequestId(Long maintenanceRequestId);

    /**
     * Find files by file type
     */
    List<MaintenanceRequestFile> findByFileType(MaintenanceRequestFile.AttachmentType fileType);



    /**
     * Find file by stored filename
     */
    Optional<MaintenanceRequestFile> findByFileName(String fileName);

    /**
     * Find files uploaded within a date range
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE f.uploadedAt BETWEEN :startDate AND :endDate")
    List<MaintenanceRequestFile> findByUploadedAtBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find files larger than specified size
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE f.fileSize > :size")
    List<MaintenanceRequestFile> findByFileSizeGreaterThan(@Param("size") Long size);

    /**
     * Find files by maintenance request and file type
     */
    List<MaintenanceRequestFile> findByMaintenanceRequestAndFileType(
            MaintenanceRequest maintenanceRequest, 
            MaintenanceRequestFile.AttachmentType fileType);

    /**
     * Count files by maintenance request
     */
    @Query("SELECT COUNT(f) FROM MaintenanceRequestFile f WHERE f.maintenanceRequest = :maintenanceRequest")
    Long countByMaintenanceRequest(@Param("maintenanceRequest") MaintenanceRequest maintenanceRequest);

    /**
     * Count files by file type
     */
    Long countByFileType(MaintenanceRequestFile.AttachmentType fileType);

    /**
     * Get total file size for a maintenance request
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM MaintenanceRequestFile f WHERE f.maintenanceRequest = :maintenanceRequest")
    Long getTotalFileSizeByMaintenanceRequest(@Param("maintenanceRequest") MaintenanceRequest maintenanceRequest);

    /**
     * Find orphaned files (files without associated maintenance request)
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE f.maintenanceRequest IS NULL")
    List<MaintenanceRequestFile> findOrphanedFiles();

    /**
     * Find files uploaded before a certain date (for cleanup)
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE f.uploadedAt < :cutoffDate")
    List<MaintenanceRequestFile> findFilesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete files by maintenance request
     */
    void deleteByMaintenanceRequest(MaintenanceRequest maintenanceRequest);

    /**
     * Delete files by maintenance request ID
     */
    void deleteByMaintenanceRequestId(Long maintenanceRequestId);

    /**
     * Find recent files (last N days)
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE f.uploadedAt >= :sinceDate ORDER BY f.uploadedAt DESC")
    List<MaintenanceRequestFile> findRecentFiles(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Search files by original filename (case-insensitive)
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :filename, '%'))")
    List<MaintenanceRequestFile> searchByOriginalFilename(@Param("filename") String filename);

    /**
     * Find files with description containing keyword
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MaintenanceRequestFile> findByDescriptionContaining(@Param("keyword") String keyword);

    /**
     * Get file statistics
     */
    @Query("SELECT " +
           "COUNT(f) as totalFiles, " +
           "COALESCE(SUM(f.fileSize), 0) as totalSize, " +
           "COALESCE(AVG(f.fileSize), 0) as averageSize " +
           "FROM MaintenanceRequestFile f")
    Object[] getFileStatistics();

    /**
     * Get file type distribution
     */
    @Query("SELECT f.fileType, COUNT(f) FROM MaintenanceRequestFile f GROUP BY f.fileType")
    List<Object[]> getFileTypeDistribution();

    /**
     * Find files by multiple criteria
     */
    @Query("SELECT f FROM MaintenanceRequestFile f WHERE " +
            "(:fileType IS NULL OR f.attachmentType = :fileType) AND " + // Corrected field name
            "(:minSize IS NULL OR f.fileSize >= :minSize) AND " +
            "(:maxSize IS NULL OR f.fileSize <= :maxSize) AND " +
            "(:startDate IS NULL OR f.uploadedAt >= :startDate) AND " +
            "(:endDate IS NULL OR f.uploadedAt <= :endDate)")
    List<MaintenanceRequestFile> findByCriteria(
            @Param("fileType") MaintenanceRequestFile.AttachmentType fileType,
            @Param("minSize") Long minSize,
            @Param("maxSize") Long maxSize,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
