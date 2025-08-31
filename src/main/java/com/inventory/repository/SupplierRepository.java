package com.inventory.repository;

import com.inventory.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    Page<Supplier> findAllByIsActiveTrue(Pageable pageable);
    Page<Supplier> findByIsActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
    List<Supplier> findByIsActiveTrueAndNameContainingIgnoreCase(String name);
    Optional<Supplier> findByIdAndIsActiveTrue(Integer id);

    // Search methods for global search functionality
    List<Supplier> findByNameContainingIgnoreCaseOrContactPersonContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String contactPerson, String email, Pageable pageable);
    
    // Search methods for global search functionality - only active suppliers
    List<Supplier> findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndContactPersonContainingIgnoreCaseOrIsActiveTrueAndEmailContainingIgnoreCase(
            String name, String contactPerson, String email, Pageable pageable);
}
