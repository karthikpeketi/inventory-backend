package com.inventory.controller;

import com.inventory.dto.SearchResultDto;
import com.inventory.service.SearchService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling search operations across different entities
 * Provides global search functionality for products, orders, suppliers, categories, and users
 */
@RestController
@RequestMapping("/api/search")
@AllArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Global search endpoint that searches across all entities
     * @param query The search query string
     * @param limit Optional limit for results per entity (default: 5)
     * @return SearchResultDto containing categorized search results
     */
    @GetMapping("/global")
    public ResponseEntity<SearchResultDto> globalSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "entityType", defaultValue = "all") String entityType,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "stockLevel", required = false) String stockLevel) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        SearchResultDto results = searchService.globalSearch(
            query.trim(), limit, entityType, dateRange, status, category, minPrice, maxPrice, stockLevel);
        return ResponseEntity.ok(results);
    }

    /**
     * Search products only
     * @param query The search query string
     * @param limit Optional limit for results (default: 10)
     * @return List of matching products
     */
    @GetMapping("/products")
    public ResponseEntity<?> searchProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(searchService.searchProducts(query.trim(), limit));
    }

    /**
     * Search orders only
     * @param query The search query string
     * @param limit Optional limit for results (default: 10)
     * @return List of matching orders
     */
    @GetMapping("/orders")
    public ResponseEntity<?> searchOrders(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(searchService.searchOrders(query.trim(), limit));
    }

    /**
     * Search suppliers only
     * @param query The search query string
     * @param limit Optional limit for results (default: 10)
     * @return List of matching suppliers
     */
    @GetMapping("/suppliers")
    public ResponseEntity<?> searchSuppliers(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(searchService.searchSuppliers(query.trim(), limit));
    }

    /**
     * Search categories only
     * @param query The search query string
     * @param limit Optional limit for results (default: 10)
     * @return List of matching categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> searchCategories(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(searchService.searchCategories(query.trim(), limit));
    }

    /**
     * Search users only (admin only)
     * @param query The search query string
     * @param limit Optional limit for results (default: 10)
     * @return List of matching users
     */
    @GetMapping("/users")
    public ResponseEntity<?> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(searchService.searchUsers(query.trim(), limit));
    }
}