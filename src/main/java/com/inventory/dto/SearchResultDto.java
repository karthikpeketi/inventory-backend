package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for returning categorized search results from global search
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDto {
    
    private List<ProductDto> products;
    private List<PurchaseOrderDto> orders;
    private List<SupplierDto> suppliers;
    private List<CategoryDto> categories;
    private List<UserDto> users;
    
    /**
     * Get total count of all search results
     * @return Total number of results across all categories
     */
    public int getTotalCount() {
        return (products != null ? products.size() : 0) +
               (orders != null ? orders.size() : 0) +
               (suppliers != null ? suppliers.size() : 0) +
               (categories != null ? categories.size() : 0) +
               (users != null ? users.size() : 0);
    }
    
    /**
     * Check if search returned any results
     * @return true if any category has results
     */
    public boolean hasResults() {
        return getTotalCount() > 0;
    }
}