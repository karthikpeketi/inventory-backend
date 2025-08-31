package com.inventory.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Category entity.
 * Used to transfer category data between layers of the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto implements Serializable {
    
    private Integer id;
    private String name;
    private String description;
    private Long productCount;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    
    // Formatted date strings for direct display in UI
    private String createdAtFormatted;
    private String updatedAtFormatted;
}