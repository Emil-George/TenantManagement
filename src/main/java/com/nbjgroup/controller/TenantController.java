package com.nbjgroup.controller;

import com.nbjgroup.entity.Tenant;
import com.nbjgroup.entity.User;
import com.nbjgroup.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nbjgroup.dto.TenantResponseDTO;
import com.nbjgroup.dto.UserDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tenants" )
@CrossOrigin(origins = "*", maxAge = 3600)
public class TenantController {

    // 1. Inject the repository to get access to database queries.
    @Autowired
    private TenantRepository tenantRepository;


    private TenantResponseDTO convertToDTO(Tenant tenant) {
        if (tenant == null) {
            return null;
        }

        // Convert the User entity to UserDTO
        User user = tenant.getUser();
        UserDTO userDTO = new UserDTO();
        if (user != null) {
            userDTO.setId(user.getId());
            userDTO.setFirstName(user.getFirstName());
            userDTO.setLastName(user.getLastName());
            userDTO.setEmail(user.getEmail());
            userDTO.setPhoneNumber(user.getPhoneNumber());
        }

        // Convert the Tenant entity to TenantResponseDTO
        TenantResponseDTO tenantDTO = new TenantResponseDTO();
        tenantDTO.setId(tenant.getId());
        tenantDTO.setUser(userDTO); // Set the nested DTO
        tenantDTO.setPropertyAddress(tenant.getPropertyAddress());
        tenantDTO.setUnitNumber(tenant.getUnitNumber());
        tenantDTO.setStatus(tenant.getStatus().name());
        tenantDTO.setRentAmount(tenant.getRentAmount());
        tenantDTO.setLeaseStartDate(tenant.getLeaseStartDate());
        tenantDTO.setLeaseEndDate(tenant.getLeaseEndDate());

        return tenantDTO;
    }

    // 2. ADD THIS ENTIRE METHOD
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            // Create sorting object based on request parameters
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            // Create a pageable object for the database query
            Pageable pageable = PageRequest.of(page, size, sort);

            // Fetch the paginated data from the repository
            // We use a custom query to also fetch the associated User details efficiently
            Page<Tenant> tenantsPage = tenantRepository.findAllWithUserDetailsPaginated(pageable);

            // Create a structured response map to send back to the frontend
            List<TenantResponseDTO> tenantDTOs = tenantsPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("tenants", tenantDTOs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Basic error handling
            // Consider adding logging here: logger.error("Error fetching tenants", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "An error occurred while fetching tenants.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ... any other methods in your controller can go here ...


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTenant(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            // Find the tenant to update
            Tenant tenant = tenantRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));

            // Partially update fields based on what's in the request body
            if (updates.containsKey("firstName")) {
                tenant.getUser().setFirstName((String) updates.get("firstName"));
            }
            if (updates.containsKey("lastName")) {
                tenant.getUser().setLastName((String) updates.get("lastName"));
            }
            if (updates.containsKey("phoneNumber")) {
                tenant.getUser().setPhoneNumber((String) updates.get("phoneNumber"));
            }
            if (updates.containsKey("status")) {
                // Convert string to enum for the status field
                String statusStr = (String) updates.get("status");
                tenant.setStatus(Tenant.TenantStatus.valueOf(statusStr.toUpperCase()));
            }

            // Save the updated tenant. Because the User is part of the Tenant,
            // changes to the user will be saved automatically (due to cascading).
            Tenant updatedTenant = tenantRepository.save(tenant);

            return ResponseEntity.ok(convertToDTO(updatedTenant));

        } catch (Exception e) {
            // logger.error("Error updating tenant with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "An error occurred while updating the tenant."));
        }
    }
}
