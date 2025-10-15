package com.nbjgroup.controller;

import com.nbjgroup.dto.TenantResponseDTO;
import com.nbjgroup.dto.TenantUpdateDTO;
import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.TenantRepository;
import com.nbjgroup.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tenants" )
@CrossOrigin(origins = "*", maxAge = 3600) // For development, be more specific in production
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    // Use constructor-based injection - it's a best practice
    @Autowired
    public TenantController(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/tenants
     * Fetches a paginated list of all tenants. (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Tenant> tenantsPage = tenantRepository.findAll(pageable); // Use the standard findAll

            // Convert the Page of entities to a Page of DTOs
            Page<TenantResponseDTO> dtoPage = tenantsPage.map(this::convertToDTO);

            // Return a structured response with pagination details
            Map<String, Object> response = new HashMap<>();
            response.put("tenants", dtoPage.getContent());
            response.put("currentPage", dtoPage.getNumber());
            response.put("totalItems", dtoPage.getTotalElements());
            response.put("totalPages", dtoPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "An error occurred while fetching tenants."));
        }
    }

    /**
     * PUT /api/tenants/{id}
     * Updates a tenant's information. (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTenant(@PathVariable Long id, @Valid @RequestBody TenantUpdateDTO tenantUpdateDTO) {
        try {
            Tenant tenant = tenantRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));

            User user = tenant.getUser();

            if (tenantUpdateDTO.getFirstName() != null) {
                user.setFirstName(tenantUpdateDTO.getFirstName());
            }
            if (tenantUpdateDTO.getLastName() != null) {
                user.setLastName(tenantUpdateDTO.getLastName());
            }
            if (tenantUpdateDTO.getEmail() != null) {
                user.setEmail(tenantUpdateDTO.getEmail());
            }
            if (tenantUpdateDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(tenantUpdateDTO.getPhoneNumber());
            }
            if (tenantUpdateDTO.getPropertyAddress() != null) {
                tenant.setPropertyAddress(tenantUpdateDTO.getPropertyAddress());
            }

            if (tenantUpdateDTO.getStatus() != null) {
                tenant.setStatus(tenantUpdateDTO.getStatus().equalsIgnoreCase("ACTIVE")?Tenant.TenantStatus.ACTIVE : Tenant.TenantStatus.INACTIVE );
            }


            userRepository.save(user);
            Tenant updatedTenant = tenantRepository.save(tenant);

            TenantResponseDTO responseDTO = convertToDTO(updatedTenant);
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "Failed to update tenant: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/tenants/{id}
     * Deletes a tenant. (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTenant(@PathVariable Long id) {
        if (!tenantRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Tenant not found with id: " + id));
        }
        try {
            // Note: This is a hard delete. For production, you might prefer a soft delete
            // (e.g., setting a 'deleted' flag). This also assumes that cascading deletes
            // are handled correctly for related entities like User. If not, you must
            // delete the associated User as well.
            tenantRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Tenant deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "Failed to delete tenant: " + e.getMessage()));
        }
    }

    /**
     * Helper method to convert a Tenant entity to a TenantResponseDTO.
     * This is crucial for preventing JSON serialization issues with Hibernate proxies
     * and for controlling the exact data sent to the frontend.
     */
    private TenantResponseDTO convertToDTO(Tenant tenant) {
        TenantResponseDTO dto = new TenantResponseDTO();
        dto.setId(tenant.getId());
        dto.setPropertyAddress(tenant.getPropertyAddress());
        dto.setUnitNumber(tenant.getUnitNumber());
        dto.setStatus(tenant.getStatus().name());
        dto.setCreatedAt(tenant.getCreatedAt());

        // Safely map user data
        if (tenant.getUser() != null) {
            TenantResponseDTO.UserDTO userDTO = new TenantResponseDTO.UserDTO();
            userDTO.setId(tenant.getUser().getId());
            userDTO.setFirstName(tenant.getUser().getFirstName());
            userDTO.setLastName(tenant.getUser().getLastName());
            userDTO.setEmail(tenant.getUser().getEmail());
            userDTO.setPhoneNumber(tenant.getUser().getPhoneNumber());
            dto.setUser(userDTO);
        }

        return dto;
    }
}
