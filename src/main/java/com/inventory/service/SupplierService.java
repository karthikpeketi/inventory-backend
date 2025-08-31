package com.inventory.service;

import com.inventory.dto.SupplierDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SupplierService {
    Page<SupplierDto> getSuppliers(int page, int size, String search);
    Page<SupplierDto> getSuppliers(int page, int size, String sortBy, String sortDirection, String search);
    List<SupplierDto> searchSuppliersByName(String name);
    SupplierDto updateSupplier(Integer id, SupplierDto supplierDto);
    void deleteSupplier(Integer id);
    SupplierDto addSupplier(SupplierDto supplierDto);
}
