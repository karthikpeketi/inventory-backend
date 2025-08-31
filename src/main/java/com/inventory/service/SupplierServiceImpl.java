package com.inventory.service;

import com.inventory.dto.SupplierDto;
import com.inventory.exception.BusinessValidationException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Supplier;
import com.inventory.repository.PurchaseOrderRepository;
import com.inventory.repository.SupplierRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    private SupplierDto toDto(Supplier s) {
        SupplierDto dto = new SupplierDto();
        BeanUtils.copyProperties(s, dto);
        return dto;
    }
    private void copyDtoToEntity(SupplierDto dto, Supplier s) {
        s.setName(dto.getName());
        s.setContactPerson(dto.getContactPerson());
        s.setEmail(dto.getEmail());
        s.setPhone(dto.getPhone());
        s.setAddress(dto.getAddress());
    }

    @Override
    public Page<SupplierDto> getSuppliers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (search != null && !search.isEmpty()) {
            return supplierRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search, pageable).map(this::toDto);
        }
        return supplierRepository.findAllByIsActiveTrue(pageable).map(this::toDto);
    }

    @Override
    public Page<SupplierDto> getSuppliers(int page, int size, String sortBy, String sortDirection, String search) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        if (search != null && !search.isEmpty()) {
            return supplierRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search, pageable).map(this::toDto);
        }
        return supplierRepository.findAllByIsActiveTrue(pageable).map(this::toDto);
    }

    @Override
    public List<SupplierDto> searchSuppliersByName(String name) {
        return supplierRepository.findByIsActiveTrueAndNameContainingIgnoreCase(name)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierDto updateSupplier(Integer id, SupplierDto supplierDto) {
        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
        copyDtoToEntity(supplierDto, supplier);
        // updatedAt handled by @UpdateTimestamp
        supplierRepository.save(supplier);
        return toDto(supplier);
    }

    @Override
    public void deleteSupplier(Integer id) {
        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
            
        // Check for pending or processing purchase orders
        List<String> activeOrderStatuses = Arrays.asList("PENDING", "PROCESSING");
        int activeOrdersCount = purchaseOrderRepository.findBySupplierAndStatusIn(supplier, activeOrderStatuses).size();
        
        if (activeOrdersCount > 0) {
            throw new BusinessValidationException("Cannot delete supplier with ID " + id + 
                " because there are " + activeOrdersCount + " active purchase orders (PENDING or PROCESSING) " +
                "associated with this supplier. Please complete or cancel these orders before deleting the supplier.");
        }
        
        // If no active orders, proceed with deactivation
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
    }

    @Override
    public SupplierDto addSupplier(SupplierDto supplierDto) {
        Supplier supplier = new Supplier();
        copyDtoToEntity(supplierDto, supplier);
        supplier.setIsActive(true);
        Supplier saved = supplierRepository.save(supplier);
        return toDto(saved);
    }
}
