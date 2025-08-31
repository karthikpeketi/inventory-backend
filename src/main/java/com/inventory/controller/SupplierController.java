package com.inventory.controller;

import com.inventory.dto.SupplierDto;
import com.inventory.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public Page<SupplierDto> getSuppliers(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String sort,
                                          @RequestParam(required = false) String search) {
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            String sortBy = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "asc";
            return supplierService.getSuppliers(page, size, sortBy, sortDirection, search);
        }
        return supplierService.getSuppliers(page, size, search);
    }

    @GetMapping("/search")
    public List<SupplierDto> searchSuppliers(@RequestParam String name) {
        return supplierService.searchSuppliersByName(name);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDto> updateSupplier(@PathVariable Integer id,
                                                      @RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Integer id) {
        supplierService.deleteSupplier(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping
    public ResponseEntity<SupplierDto> addSupplier(@RequestBody SupplierDto supplierDto) {
        return new ResponseEntity<>(supplierService.addSupplier(supplierDto), HttpStatus.CREATED);
    }
}
