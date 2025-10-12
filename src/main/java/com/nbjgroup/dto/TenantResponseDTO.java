package com.nbjgroup.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// This DTO represents the Tenant data sent back to the frontend.
public class TenantResponseDTO {
    private Long id;
    private UserDTO user; // It contains the UserDTO
    private String propertyAddress;
    private String unitNumber;
    private String status;
    private BigDecimal rentAmount;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;

    // --- Getters and Setters for all fields ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getRentAmount() { return rentAmount; }
    public void setRentAmount(BigDecimal rentAmount) { this.rentAmount = rentAmount; }
    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }
    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }
}
