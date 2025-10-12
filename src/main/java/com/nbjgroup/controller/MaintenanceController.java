package com.nbjgroup.controller;

import com.nbjgroup.dto.maintenance.MaintenanceRequestDTO;
import com.nbjgroup.entity.MaintenanceRequest;
import com.nbjgroup.entity.MaintenanceRequestFile;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.MaintenanceRequestFileRepository;
import com.nbjgroup.repository.MaintenanceRequestRepository;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import com.nbjgroup.service.MaintenanceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/maintenance" )
@CrossOrigin(origins = "*", maxAge = 3600)
public class MaintenanceController {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);

    @Autowired
    private MaintenanceRequestRepository maintenanceRepository;

    @Autowired
    private MaintenanceRequestFileRepository fileRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceService maintenanceService;

    @Value("${app.upload.dir:${user.home}/nbj-uploads}")
    private String uploadDir;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllMaintenanceRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<MaintenanceRequest> requests;

            if (status != null && priority != null) {
                MaintenanceRequest.RequestStatus statusEnum = MaintenanceRequest.RequestStatus.valueOf(status.toUpperCase());
                MaintenanceRequest.Priority priorityEnum = MaintenanceRequest.Priority.valueOf(priority.toUpperCase());
                requests = maintenanceRepository.findByStatusAndPriority(statusEnum, priorityEnum, pageable);
            } else if (status != null) {
                MaintenanceRequest.RequestStatus statusEnum = MaintenanceRequest.RequestStatus.valueOf(status.toUpperCase());
                requests = maintenanceRepository.findByStatus(statusEnum, pageable);
            } else if (priority != null) {
                MaintenanceRequest.Priority priorityEnum = MaintenanceRequest.Priority.valueOf(priority.toUpperCase());
                requests = maintenanceRepository.findByPriorityOrderByCreatedAtDesc(priorityEnum, pageable);
            } else {
                requests = maintenanceRepository.findAll(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
            response.put("currentPage", requests.getNumber());
            response.put("totalItems", requests.getTotalElements());
            response.put("totalPages", requests.getTotalPages());
            response.put("pageSize", requests.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching maintenance requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching maintenance requests", "FETCH_ERROR"));
        }
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> getMyMaintenanceRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not found", "USER_NOT_FOUND"));
            }

            Optional<Tenant> tenantOpt = tenantRepository.findByUser(userOpt.get());
            if (tenantOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Tenant profile not found", "TENANT_NOT_FOUND"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<MaintenanceRequest> requests = maintenanceRepository.findByTenant(tenantOpt.get(), pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
            response.put("currentPage", requests.getNumber());
            response.put("totalItems", requests.getTotalElements());
            response.put("totalPages", requests.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching tenant maintenance requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching maintenance requests", "FETCH_ERROR"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMaintenanceRequest(@PathVariable Long id) {
        try {
            Optional<MaintenanceRequest> requestOpt = maintenanceRepository.findById(id);

            if (requestOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MaintenanceRequest request = requestOpt.get();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!canAccessRequest(request, auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied", "ACCESS_DENIED"));
            }

            return ResponseEntity.ok(convertToDTO(request));

        } catch (Exception e) {
            logger.error("Error fetching maintenance request ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching maintenance request", "FETCH_ERROR"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> createMaintenanceRequest(@Valid @RequestBody MaintenanceRequestDTO requestDTO) {
        try {
            // Get the currently logged-in user's email from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // Delegate all business logic to the service layer
            MaintenanceRequest createdRequest = maintenanceService.createMaintenanceRequest(requestDTO, userEmail);

            // Return the created object with a 201 Created status
            return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);

        } catch (IllegalStateException | UsernameNotFoundException e) {
            // Handle cases where the user or tenant profile isn't found
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log the error for debugging
            // logger.error("Error creating maintenance request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        try {
            Optional<MaintenanceRequest> requestOpt = maintenanceRepository.findById(id);

            if (requestOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MaintenanceRequest request = requestOpt.get();

            if (updates.containsKey("status")) {
                String statusStr = (String) updates.get("status");
                request.setStatus(MaintenanceRequest.RequestStatus.valueOf(statusStr.toUpperCase()));
            }

            if (updates.containsKey("assignedTo")) {
                request.setAssignedTo((String) updates.get("assignedTo"));
            }

            if (updates.containsKey("adminNotes")) {
                request.setAdminNotes((String) updates.get("adminNotes"));
            }

            if (updates.containsKey("scheduledDate")) {
                String dateStr = (String) updates.get("scheduledDate");
                request.setScheduledDate(LocalDateTime.parse(dateStr));
            }

            if (request.getStatus() == MaintenanceRequest.RequestStatus.COMPLETED && request.getCompletedAt() == null) {
                request.setCompletedAt(LocalDateTime.now());
            }

            request.setUpdatedAt(LocalDateTime.now());
            MaintenanceRequest updatedRequest = maintenanceRepository.save(request);

            logger.info("Maintenance request updated - ID: {}, Status: {}", id, request.getStatus());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Maintenance request updated successfully");
            response.put("request", convertToDTO(updatedRequest));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(convertToDTO(updatedRequest));

        } catch (Exception e) {
            logger.error("Error updating maintenance request ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating maintenance request", "UPDATE_ERROR"));
        }
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> addFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> feedback) {

        try {
            Optional<MaintenanceRequest> requestOpt = maintenanceRepository.findById(id);

            if (requestOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MaintenanceRequest request = requestOpt.get();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!canAccessRequest(request, auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied", "ACCESS_DENIED"));
            }
            if (request.getStatus() != MaintenanceRequest.RequestStatus.COMPLETED) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Can only provide feedback on completed requests", "INVALID_STATUS"));
            }

            if (feedback.containsKey("rating")) {
                Integer rating = (Integer) feedback.get("rating");
                if (rating < 1 || rating > 5) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Rating must be between 1 and 5", "INVALID_RATING"));
                }
                request.setTenantRating(rating);
            }

            if (feedback.containsKey("tenantFeedback")) {
                request.setTenantFeedback((String) feedback.get("tenantFeedback"));
            }

            request.setUpdatedAt(LocalDateTime.now());
            MaintenanceRequest updatedRequest = maintenanceRepository.save(request);

            logger.info("Feedback added to maintenance request - ID: {}, Rating: {}", id, request.getTenantRating());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feedback added successfully");
            response.put("request", convertToDTO(updatedRequest));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error adding feedback to maintenance request ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error adding feedback", "FEEDBACK_ERROR"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMaintenanceRequest(@PathVariable Long id) {
        try {
            Optional<MaintenanceRequest> requestOpt = maintenanceRepository.findById(id);

            if (requestOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MaintenanceRequest request = requestOpt.get();

            for (MaintenanceRequestFile file : request.getAttachments()) {
                try {
                    Path filePath = Paths.get(file.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                } catch (IOException e) {
                    logger.warn("Could not delete file: {}", file.getFilePath(), e);
                }
            }

            maintenanceRepository.delete(request);

            logger.info("Maintenance request deleted - ID: {}", id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Maintenance request deleted successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting maintenance request ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deleting maintenance request", "DELETE_ERROR"));
        }
    }

    private List<MaintenanceRequestFile> handleFileUploads(MultipartFile[] files, MaintenanceRequest request)
            throws IOException {

        List<MaintenanceRequestFile> uploadedFiles = new ArrayList<>();
        Path uploadPath = Paths.get(uploadDir, "maintenance");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String uniqueFilename = generateUniqueFilename(originalFilename);
                Path filePath = uploadPath.resolve(uniqueFilename);

                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                MaintenanceRequestFile fileRecord = new MaintenanceRequestFile();
                fileRecord.setOriginalFileName(originalFilename);
                fileRecord.setFileName(uniqueFilename); // Use 'fileName' for the stored name
                fileRecord.setFilePath(filePath.toString());
                fileRecord.setFileSize(file.getSize());
                fileRecord.setAttachmentType(determineFileType(file.getContentType()));
                fileRecord.setMaintenanceRequest(request);
                fileRecord.setUploadedAt(LocalDateTime.now());

                uploadedFiles.add(fileRecord);
            }
        }
        return fileRepository.saveAll(uploadedFiles);
    }

    private boolean canAccessRequest(MaintenanceRequest request, Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        String username = auth.getName();
        return request.getTenant().getUser().getEmail().equals(username);
    }

    private Map<String, Object> convertToDTO(MaintenanceRequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", request.getId());
        dto.put("title", request.getTitle());
        dto.put("description", request.getDescription());
        dto.put("category", request.getCategory());
        dto.put("priority", request.getPriority().toString());
        dto.put("status", request.getStatus().toString());
        dto.put("assignedTo", request.getAssignedTo());
        dto.put("adminNotes", request.getAdminNotes());
        dto.put("tenantFeedback", request.getTenantFeedback());
        dto.put("rating", request.getTenantRating());
        dto.put("createdAt", request.getCreatedAt());
        dto.put("updatedAt", request.getUpdatedAt());
        dto.put("scheduledDate", request.getScheduledDate());
        dto.put("completedAt", request.getCompletedAt());

        Map<String, Object> tenantInfo = new HashMap<>();
        tenantInfo.put("email", request.getTenant().getUser().getEmail());
        tenantInfo.put("phone", request.getTenant().getEmergencyContactPhone());
        tenantInfo.put("propertyAddress", request.getTenant().getPropertyAddress());
        dto.put("tenant", tenantInfo);

        List<Map<String, Object>> filesInfo = request.getAttachments().stream()
                .map(file -> {
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("id", file.getId());
                    fileMap.put("originalFilename", file.getOriginalFileName());
                    fileMap.put("fileType", file.getAttachmentType());
                    fileMap.put("fileSize", file.getFileSize());
                    fileMap.put("uploadedAt", file.getUploadedAt());
                    fileMap.put("downloadUrl", file.getDownloadUrl());
                    fileMap.put("thumbnailUrl", file.getThumbnailUrl());
                    fileMap.put("viewUrl", "/api/files/" + file.getId());
                    return fileMap;
                })
                .collect(Collectors.toList());
        dto.put("files", filesInfo);

        return dto;
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private MaintenanceRequestFile.AttachmentType determineFileType(String contentType) {
        if (contentType == null) {
            return MaintenanceRequestFile.AttachmentType.OTHER;
        }

        if (contentType.startsWith("image/")) {
            return MaintenanceRequestFile.AttachmentType.IMAGE;
        } else if (contentType.startsWith("application/") || contentType.startsWith("text/")) {
            return MaintenanceRequestFile.AttachmentType.DOCUMENT;
        } else {
            return MaintenanceRequestFile.AttachmentType.OTHER;
        }
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}
