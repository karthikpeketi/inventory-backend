package com.inventory.repository;


import com.inventory.model.Category;
import com.inventory.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    Optional<Product> findBySku(String sku);
    Optional<Product> findByBarcode(String barcode);
    List<Product> findByCategoryId(Integer categoryId);
    
    // Corrected search method with active flag
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
        
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel AND p.isActive = true")
    List<Product> findLowStockProducts();
    
    // Count active products
    long countByIsActiveTrue();
    
    // Get products by category with active status
    List<Product> findByCategoryIdAndIsActiveTrue(Integer categoryId);

    Optional<Product> findByIdAndIsActiveTrue(Integer id);

    Optional<Product> findByBarcodeAndIsActiveTrue(String barcode);

    // Regular list version (non-paginated)
    List<Product> findByIsActiveTrue();
    
    // Sorted version (non-paginated)
    List<Product> findByIsActiveTrue(Sort sort);
    
    // Paginated version
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // New methods for paginated search and filter
    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseAndIsActiveTrue(
            String name, String sku, Pageable pageable);

    Page<Product> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);

    // Fixed method with proper grouping using @Query annotation
    @Query("SELECT p FROM Product p WHERE (p.name LIKE %:name% OR p.sku LIKE %:sku%) AND p.category = :category AND p.isActive = true")
    Page<Product> findByNameOrSkuContainingAndCategoryAndIsActiveTrue(
            @Param("name") String name, @Param("sku") String sku, @Param("category") Category category, Pageable pageable);

    // Find active products by name containing (case-insensitive) and category
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.category = :category AND p.isActive = true")
    Page<Product> findByNameContainingIgnoreCaseAndCategoryAndIsActiveTrue(
            @Param("name") String name, @Param("category") Category category, Pageable pageable);

    // Search methods for global search functionality
    List<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String sku, Pageable pageable);
    
    // Search methods for global search functionality - only active products
    List<Product> findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndSkuContainingIgnoreCase(
            String name, String sku, Pageable pageable);

}