package com.nbjgroup.controller;

import com.nbjgroup.entity.MaintenanceRequestFile;
import com.nbjgroup.repository.MaintenanceRequestFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/files" )
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private MaintenanceRequestFileRepository fileRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getFileById(@PathVariable Long id) {
        Optional<MaintenanceRequestFile> fileOpt = fileRepository.findById(id);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("File not found", "FILE_NOT_FOUND"));
        }

        MaintenanceRequestFile file = fileOpt.get();
        return ResponseEntity.ok(convertFileToDTO(file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Optional<MaintenanceRequestFile> fileOpt = fileRepository.findById(id);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MaintenanceRequestFile file = fileOpt.get();
        try {
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                        .contentType(MediaType.parseMediaType(String.valueOf(file.getFileType()))) // Use getFileType() for content type
                        .body(resource);
            } else {
                logger.error("File not found or not readable: {}", file.getFilePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException e) {
            logger.error("Error creating URL for file path: {}", file.getFilePath(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> convertFileToDTO(MaintenanceRequestFile file) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", file.getId());
        dto.put("originalFilename", file.getOriginalFileName());
        dto.put("storedFilename", file.getFileName());
        dto.put("fileType", file.getAttachmentType().toString());
        dto.put("fileSize", file.getFileSize());
        dto.put("uploadedAt", file.getUploadedAt());
        dto.put("downloadUrl", file.getDownloadUrl());
        dto.put("thumbnailUrl", file.getThumbnailUrl());
        return dto;
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);
        return errorResponse;
    }
}
