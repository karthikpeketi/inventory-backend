package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * SellProductRequest
 * 
 * DTO for handling product sales requests.
 * Contains product ID, quantity to sell, and optional notes.
 */
public class SellProductRequest {
    
    @NotNull(message = "Product ID is required")
    private Integer productId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    
    private String notes;

    // Getters and setters
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
