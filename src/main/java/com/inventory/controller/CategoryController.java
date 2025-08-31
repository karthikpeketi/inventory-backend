package com.inventory.controller;

import com.inventory.dto.CategoryDto;
import com.inventory.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CategoryController
 * 
 * Handles all REST API requests related to category management within the inventory system.
 * 
 * High-level responsibilities:
 * - List, create, update, and delete categories
 * - Provide category data for product filtering and organization
 * - Admin endpoint protection (write operations restricted to admins)
 * - Pagination and search functionality for categories
 *
 * All endpoints are prefixed with /api/categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Get all categories (for dropdowns, etc.)
     * GET /api/categories/all
     * 
     * Returns a full list of all categories, sorted by name.
     * Used for dropdowns and other non-paginated views.
     * 
     * @return List of CategoryDto with all category details.
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    
    /**
     * Get paginated categories with optional search
     * GET /api/categories
     * 
     * Returns a paginated list of categories with sorting and optional search.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction (asc or desc)
     * @param search Optional search term
     * @return Paginated list of categories
     */
    @GetMapping
    public ResponseEntity<Page<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        
        // Create pageable object with sorting
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated categories
        Page<CategoryDto> categories = categoryService.getCategories(pageable, search);
        return ResponseEntity.ok(categories);
    }

    /**
     * Get Category by ID
     * GET /api/categories/{id}
     *
     * Look up a single category by its unique identifier.
     * 
     * @param id Category ID from the request path
     * @return CategoryDto for a matching category, or 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
    
    /**
     * Create a New Category [Admin only]
     * POST /api/categories
     *
     * Adds a new category to the system.
     * 
     * Security: Only users with ADMIN role may perform this action.
     * 
     * @param categoryDto Category data from the client (JSON in POST body), validated.
     * @return Created CategoryDto (with assigned ID, etc.)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        CategoryDto createdCategory = categoryService.createCategory(categoryDto);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    /**
     * Update an Existing Category [Admin only]
     * PUT /api/categories/{id}
     *
     * Replaces the information for an existing category.
     * 
     * Security: Admin only.
     * 
     * @param id Path variable for which category to update
     * @param categoryDto New category data from client
     * @return Updated CategoryDto after save
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Integer id, 
            @Valid @RequestBody CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDto));
    }

    /**
     * Delete a Category [Admin only]
     * DELETE /api/categories/{id}
     *
     * Removes a category from the system.
     * 
     * @param id Category to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
