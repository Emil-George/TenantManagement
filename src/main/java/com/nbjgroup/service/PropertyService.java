package com.nbjgroup.service;

import com.nbjgroup.dto.PropertyDTO;
import com.nbjgroup.entity.Property;
import com.nbjgroup.repository.PropertyRepository;
import com.nbjgroup.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<PropertyDTO> getAllProperties() {
        return propertyRepository.findAll().stream().map(property -> {
            PropertyDTO dto = new PropertyDTO(property);
            long currentTenants = tenantRepository.countByProperty_Id(property.getId());
            dto.setCurrentTenantsCount(currentTenants);
            dto.setVacanciesCount(property.getNumberOfUnits() - currentTenants);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PropertyDTO> getPropertyById(Long id) {
        return propertyRepository.findById(id).map(property -> {
            PropertyDTO dto = new PropertyDTO(property);
            long currentTenants = tenantRepository.countByProperty_Id(property.getId());
            dto.setCurrentTenantsCount(currentTenants);
            dto.setVacanciesCount(property.getNumberOfUnits() - currentTenants);
            return dto;
        });
    }

    @Transactional
    public Property createProperty(PropertyDTO propertyDTO) {
        Property property = Property.builder()
                .name(propertyDTO.getName())
                .address(propertyDTO.getAddress())
                .mapLink(propertyDTO.getMapLink())
                .managerOwnerName(propertyDTO.getManagerOwnerName())
                .numberOfUnits(propertyDTO.getNumberOfUnits())
                .build();
        return propertyRepository.save(property);
    }

    @Transactional
    public Optional<Property> updateProperty(Long id, PropertyDTO propertyDTO) {
        return propertyRepository.findById(id).map(existingProperty -> {
            existingProperty.setName(propertyDTO.getName());
            existingProperty.setAddress(propertyDTO.getAddress());
            existingProperty.setMapLink(propertyDTO.getMapLink());
            existingProperty.setManagerOwnerName(propertyDTO.getManagerOwnerName());
            existingProperty.setNumberOfUnits(propertyDTO.getNumberOfUnits());
            return propertyRepository.save(existingProperty);
        });
    }

    @Transactional
    public boolean deleteProperty(Long id) {
        return propertyRepository.findById(id)
                .map(property -> {
                    propertyRepository.delete(property);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long countAllProperties() {
        return propertyRepository.count();
    }
}
