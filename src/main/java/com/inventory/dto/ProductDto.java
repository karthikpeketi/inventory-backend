package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductDto – Data Transfer Object for transferring product information between backend and frontend,
 * or between application layers.
 * 
 * Purpose:
 * - Abstracts and simplifies the product model for API communication (never exposes internal or sensitive entity fields).
 * - Typical use: return values from product-related API endpoints; also request body for create/update.
 *
 * Lombok:
 * - @Data           : Auto-generates getters, setters, equals, hashCode, and toString.
 * - @Builder        : Enables the builder pattern for easy, readable construction.
 * - @NoArgsConstructor / @AllArgsConstructor : Provide constructors for serialization and mapping tools.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    // Unique identifier for the product (primary key, or null when creating new)
    private Integer id;

    // Human-readable product name (required, e.g., "Acer Laptop XZ")
    private String name;

    // Optional detailed description text for UI/product catalogs.
    private String description;

    // SKU (Stock Keeping Unit) – unique string identifier commonly used internally
    private String sku;

    // Barcode string for physical product lookup (used in scanning, search, etc.)
    private String barcode;

    // ID of the category this product belongs to (foreign key in DB, if normalized)
    private Integer categoryId;

    // Human-readable category name (redundant but useful for faster UI construction)
    private String categoryName;

    // Current quantity of the product in inventory (may be updated by stock-in/out/trx)
    private Integer quantity;

    // Minimal quantity before reordering is advisable (for dashboard/alerts)
    private Integer reorderLevel;

    // Selling price per unit (what customers pay)
    private Double unitPrice;

    // Internal cost per unit (what the business paid to acquire or manufacture)
    private Double costPrice;

    // Web URL for product image/resource (optional, UI/display only)
    private String imageUrl;

    // Logical flag (true = visible/active/product is available to sell or update)
    private Boolean isActive;

    // Audit: when the product record was first created (automatic, set by backend)
    private LocalDateTime createdAt;

    // Audit: when the product was last updated (for concurrency checking/audit/UI display)
    private LocalDateTime updatedAt;
    
    // Formatted date strings for direct display in UI
    private String createdAtFormatted;
    private String updatedAtFormatted;
}
