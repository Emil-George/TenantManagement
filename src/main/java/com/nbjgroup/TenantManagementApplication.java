package com.nbjgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for NBJ Group Tenant Management Platform.
 * 
 * This application provides a comprehensive tenant management and rent collection
 * system with role-based authentication, file handling, and RESTful APIs.
 * 
 * Features:
 * - JWT-based authentication and authorization
 * - Role-based access control (Admin/Tenant)
 * - Tenant and lease management
 * - Rent payment tracking
 * - Maintenance request handling
 * - File upload and document management
 * 
 * @author NBJ Group Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class TenantManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantManagementApplication.class, args);
        System.out.println("=".repeat(60));
        System.out.println("🏠 NBJ Group Tenant Management Platform Started Successfully!");
        System.out.println("📍 API Base URL: http://localhost:8080/api");
        System.out.println("📚 Health Check: http://localhost:8080/api/actuator/health");
        System.out.println("🔐 Default Admin: admin / admin123");
        System.out.println("=".repeat(60));
    }
}
