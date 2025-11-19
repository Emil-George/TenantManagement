package com.nbjgroup.dto;

import com.nbjgroup.entity.Property;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyDTO {
    private Long id;
    @NotBlank(message = "Property name is required")
    private String name;
    @NotBlank(message = "Address is required")
    private String address;
    private String mapLink;
    @NotBlank(message = "Manager/Owner name is required")
    private String managerOwnerName;
    @Min(value = 0, message = "Number of units cannot be negative")
    private int numberOfUnits;
    private long currentTenantsCount;
    private long vacanciesCount;

    public PropertyDTO(Property property) {
        this.id = property.getId();
        this.name = property.getName();
        this.address = property.getAddress();
        this.mapLink = property.getMapLink();
        this.managerOwnerName = property.getManagerOwnerName();
        this.numberOfUnits = property.getNumberOfUnits();
    }
}
