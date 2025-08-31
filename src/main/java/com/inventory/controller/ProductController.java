package com.inventory.controller;

import com.inventory.dto.ProductDto;      // DTO that carries product information to and from the frontend
import com.inventory.service.ProductService; // Service containing all business logic for product management
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Used for endpoint-level security (role checks)
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProductController
 * 
 * Handles all REST API requests related to product management within the inventory system.
 * 
 * High-level responsibilities:
 * - List, create, update, search, delete, and retrieve product(s)
 * - Filter products by specific fields (name, barcode, category, stock)
 * - Pagination support for large product lists
 * - Admin endpoint protection (write operations restricted to admins)
 *
 * This controller demonstrates common REST API design, DTO use, and method-level security controls.
 * All endpoints are prefixed with /api/products.
 */
@RestController  // Registers this class as a Spring REST endpoint provider (auto-JSON conversion).
@RequestMapping("/api/products") // Prefixes all endpoints with /api/products
public class ProductController {
    
    // Dependency Injection: ProductService instance provided at runtime
    private final ProductService productService;
    
    // Spring uses this constructor to inject the service dependency
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * List All Products
     * GET /api/products
     * 
     * Returns a full list of all active products with optional sorting.
     * 
     * @param sortBy Optional field to sort by (name, sku, unitPrice, quantity, etc.)
     * @param sortDirection Optional sort direction (asc or desc), defaults to asc
     * @return List of ProductDto with all product details, sorted if parameters provided.
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(productService.getAllProducts(sortBy, sortDirection));
    }

    /**
     * Get Product by ID
     * GET /api/products/{id}
     *
     * Look up a single product by its unique identifier.
     * 
     * @param id Product ID from the request path
     * @return ProductDto (full info) for a matching product, or 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Integer id) {
        // @PathVariable maps {id} in path to parameter
        return ResponseEntity.ok(productService.getProductById(id));
    }
    
    /**
     * Create a New Product
     * POST /api/products
     *
     * Adds a new product to the inventory.
     * 
     * @param productDto Product data from the client (JSON in POST body), validated.
     * @param userId User ID from the request body for transaction tracking
     * @return Created ProductDto (with assigned ID, etc.)
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto, @RequestParam Integer userId) {
        return ResponseEntity.ok(productService.createProduct(productDto, userId));
    }

    /**
     * Update an Existing Product [Admin only]
     * PUT /api/products/{id}
     *
     * Replaces the information for an existing product.
     * 
     * Security: Admin only.
     * 
     * @param id Path variable for which product to update
     * @param productDto New product data from client
     * @return Updated ProductDto after save
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Integer id, 
            @Valid @RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.updateProduct(id, productDto));
    }

    /**
     * Delete a Product [Admin only]
     * DELETE /api/products/{id}
     *
     * Permanently removes a product from inventory. This can be replaced with a "soft delete"
     * in many real-world scenarios for data retention best practice.
     * 
     * @param id Product to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        // Return HTTP 204 No Content for successful deletion
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Search Products by Name
     * GET /api/products/search?name=...
     *
     * Case-insensitive search of product names for matching text.
     * 
     * @param name Query string parameter to search in product names
     * @return List of matching products
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchProducts(name));
    }

    /**
     * Get Low-Stock Products
     * GET /api/products/low-stock
     *
     * Returns products whose quantity is at or below their "reorder level".
     * Useful for purchase planning and dashboard alerts.
     * 
     * @return List of ProductDto needing reorder soon.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    /**
     * Get Paginated Product List
     * GET /api/products/paginated
     *
     * Supports fetching products in "pages" for efficient UI rendering of large lists.
     * 
     * Example: /api/products/paginated?page=0&size=10&sortBy=category&sortDirection=asc
     * 
     * @param search Optional search query
     * @param category Optional category filter
     * @param sortBy Optional field to sort by
     * @param sortDirection Optional sort direction (asc or desc), defaults to asc
     * @param pageable Spring's standardized pagination info taken from the query string (page, size only)
     * @return One page (subset) of products and related pagination metadata
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductDto>> getPaginatedProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getPaginatedProducts(search, category, sortBy, sortDirection, pageable));
    }
}
