package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * InventoryTransactionDto – Data Transfer Object representing a single inventory transaction.
 *
 * Purpose:
 * - Encapsulates information about a stock movement event (stock in, stock out, adjustment, etc.).
 * - Used when transferring transaction data in REST APIs, decoupling from the deeper JPA entity model.
 * - Contains only simple field values (no business logic).
 * 
 * Lombok annotations used:
 * - @Data: Auto-creates standard getters, setters, equals/hashCode, and toString.
 * - @Builder: Allows for builder-pattern construction (fluent, safe).
 * - @NoArgsConstructor: Allows frameworks to instantiate as needed.
 * - @AllArgsConstructor: Alternate constructor for all fields (handy for mapping layers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDto {
    // Unique identifier for this transaction (optional for new transactions)
    private Integer id;

    // ID of the product involved in this transaction
    private Integer productId;

    // Name of the product (copies Product.name at time of transaction, useful for display)
    private String productName;

    // ID of the user who performed the transaction (e.g., cashier, warehouse staff)
    private Integer userId;

    // Username of the actor (again, for display/logging convenience)
    private String username;
    
    // The type of transaction: (e.g., "STOCK_IN", "STOCK_OUT", or "ADJUSTMENT")
    private String transactionType;

    // The number of product units moved in this transaction (positive for in, negative/out conventions vary)
    private Integer quantity;

    // Optional notes, comments, or reason for this inventory movement
    private String notes;

    // An external/internal reference (e.g., purchase order number, sales order, external doc link)
    private String referenceNumber;

    // Date and time when the transaction occurred (assigned by the backend)
    private LocalDateTime transactionDate;
    
    // Formatted transaction date string for direct display in UI
    private String transactionDateFormatted;
    
    // Selling price for dashboard revenue calculations
    private Double unitPrice;
}
