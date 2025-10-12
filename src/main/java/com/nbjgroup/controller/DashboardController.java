package com.nbjgroup.controller;

import com.nbjgroup.dto.dashboard.TenantDashboardDTO;
import com.nbjgroup.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard" )
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<TenantDashboardDTO> getTenantDashboard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        TenantDashboardDTO data = dashboardService.getTenantDashboardData(userEmail);
        return ResponseEntity.ok(data);
    }
}
