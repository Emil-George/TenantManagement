package com.nbjgroup.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * MaintenanceRequestFile entity representing file attachments for maintenance requests.
 * Handles image uploads, documents, and other file attachments related to maintenance issues.
 */
@Entity
@Table(name = "maintenance_request_files", indexes = {
    @Index(name = "idx_maintenance_file_request", columnList = "maintenance_request_id"),
    @Index(name = "idx_maintenance_file_type", columnList = "file_type")
})
@EntityListeners(AuditingEntityListener.class)
public class MaintenanceRequestFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One relationship with MaintenanceRequest
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @NotBlank(message = "File name is required")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank(message = "Original file name is required")
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @NotBlank(message = "File path is required")
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize; // in bytes

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private AttachmentType fileType;

    @Column(name = "file_extension")
    private String fileExtension;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false)
    private AttachmentType attachmentType = AttachmentType.IMAGE;

    @Column(name = "description")
    private String description;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // Constructors
    public MaintenanceRequestFile() {}

    public MaintenanceRequestFile(MaintenanceRequest maintenanceRequest, String fileName,
                                String originalFileName, String filePath, AttachmentType fileType) {
        this.maintenanceRequest = maintenanceRequest;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileExtension = extractFileExtension(originalFileName);
        this.attachmentType = fileType;
    }

    // Business methods
    public boolean isImage() {
        return attachmentType == AttachmentType.IMAGE;
    }

    public boolean isDocument() {
        return attachmentType == AttachmentType.DOCUMENT;
    }

    public boolean isVideo() {
        return attachmentType == AttachmentType.VIDEO;
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "Unknown";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }



    public String getDownloadUrl() {
        return "/api/maintenance-requests/" + maintenanceRequest.getId() + "/files/" + id + "/download";
    }

    public String getThumbnailUrl() {
        if (isImage()) {
            return "/api/maintenance-requests/" + maintenanceRequest.getId() + "/files/" + id + "/thumbnail";
        }
        return null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MaintenanceRequest getMaintenanceRequest() {
        return maintenanceRequest;
    }

    public void setMaintenanceRequest(MaintenanceRequest maintenanceRequest) {
        this.maintenanceRequest = maintenanceRequest;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        this.fileExtension = extractFileExtension(originalFileName);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public AttachmentType getFileType() {
        return fileType;
    }

    public void setFileType(AttachmentType fileType) {
        this.fileType = fileType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceRequestFile that = (MaintenanceRequestFile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MaintenanceRequestFile{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", attachmentType=" + attachmentType +
                ", fileSize=" + fileSize +
                '}';
    }

    /**
     * Enum representing file attachment types
     */
    public enum AttachmentType {
        IMAGE("Image"),
        DOCUMENT("Document"),
        VIDEO("Video"),
        AUDIO("Audio"),
        OTHER("Other");

        private final String displayName;

        AttachmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
