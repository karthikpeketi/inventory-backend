package com.inventory.controller;

import com.inventory.dto.InventoryTransactionDto; // DTO for inventory transaction details
import com.inventory.dto.SellProductRequest; // DTO for sell product request
import com.inventory.service.InventoryService;    // Service bean that handles business logic
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * InventoryController
 * 
 * This REST controller exposes inventory operations and transaction history endpoints.
 * It follows common Spring REST API conventions, mapping HTTP requests to Java methods.
 * All core business logic is delegated to the InventoryService.
 *
 * Main responsibilities:
 * - Stock in: Add quantity to product inventory
 * - Stock out: Subtract inventory (e.g., sales, write-offs)
 * - Fetch all transactions or filter by product
 * - Get recent transactions (useful for dashboards)
 * - Calculate and expose total inventory value (all in-stock products × their price)
 * 
 * (All endpoints are prefixed with `/api/inventory`)
 */
@RestController // Tells Spring this class will handle HTTP requests and return JSON data
@RequestMapping("/api/inventory") // Sets a path prefix for all endpoints in this controller
public class InventoryController {
    
    // --- Dependency Injection ---
    // The InventoryService bean is injected into the constructor at runtime by Spring.
    // This allows for loose coupling and easy unit testing.
    private final InventoryService inventoryService;
    
    // Constructor-based dependency injection.
    // Spring will automatically inject the InventoryService bean when creating this controller.
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Get Recent Transactions
     * GET /api/inventory/transactions/recent?count=5
     *
     * Quickly fetches a limited set of the most recent transactions.
     * Count parameter allows clients to decide how many records they want (default is 5).
     *
     * @param count Number of recent transactions to fetch (defaulted via annotation)
     * @return List containing the 'count' most recent InventoryTransactionDto objects.
     */
    @GetMapping("/transactions/recent")
    public ResponseEntity<List<InventoryTransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int count) { // HTTP query parameter: /recent?count=10
        // Fetches limited number of transactions, ordered by recency
        return ResponseEntity.ok(inventoryService.getRecentTransactions(count));
    }
    
    /**
     * Get Total Inventory Value
     * GET /api/inventory/value
     *
     * Calculates the combined value of all inventory (e.g., sum of unitPrice × quantity for all in-stock products).
     * Useful for dashboards, reporting, and business analysis.
     * 
     * @return Double: The currency value representation of all stock in the system.
     */
    @GetMapping("/value")
    public ResponseEntity<Double> getInventoryValue() {
        // Calculates and returns the total monetary value of current inventory
        return ResponseEntity.ok(inventoryService.getInventoryValue());
    }
    
    /**
     * Sell Product Endpoint
     * POST /api/inventory/sell
     *
     * Allows selling a product (reducing inventory count).
     * Accessible to both ADMIN and STAFF roles.
     * 
     * @param sellRequest Object containing productId, quantity, and optional notes
     * @return The created inventory transaction for this sale
     */
    @PostMapping("/sell")
    public ResponseEntity<InventoryTransactionDto> sellProduct(
            @Valid @RequestBody SellProductRequest sellRequest) {
        InventoryTransactionDto transaction = inventoryService.sellProduct(
            sellRequest.getProductId(), 
            sellRequest.getQuantity(),
            sellRequest.getNotes()
        );
        return ResponseEntity.ok(transaction);
    }
}
