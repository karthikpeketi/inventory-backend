package com.inventory.service;

import com.inventory.dto.CategoryDto;
import com.inventory.exception.ResourceAlreadyExistsException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.util.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CategoryService
 *
 * <p>
 * Central service for managing Category entities. Handles all business logic and
 * communicates with the data layer (repository), mapping between entity and DTO
 * as needed for the application's API.
 * </p>
 * <p>
 * Service classes in Spring are intended to encapsulate the application's logic,
 * keeping controllers thin and ensuring consistency and reusability.
 * </p>
 */
@Service // Register this class as a Spring component so it can be injected into controllers, etc.
public class CategoryService {
    
    // Spring will inject a repository implementation connected to the database.
    private final CategoryRepository categoryRepository;

    // Constructor-based dependency injection for more testable and immutable code.
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retrieves all categories from the database, mapping each one from
     * the entity object to a data transfer object (DTO) for use in the API layer.
     *
     * @return List of CategoryDto representing all categories in the system.
     */
    public List<CategoryDto> getAllCategories() {
        // Get all categories sorted by name
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        
        // Get product counts for each category
        Map<Integer, Long> productCountMap = getProductCountMap();
        
        // Map to DTOs with product counts
        return categories.stream()
                .map(category -> {
                    CategoryDto dto = ModelMapper.mapToCategoryDto(category);
                    dto.setProductCount(productCountMap.getOrDefault(category.getId(), 0L));
                    return dto;
                })
                .toList();
    }
    
    /**
     * Get paginated categories with optional search
     * @param pageable Pagination information
     * @param searchQuery Optional search term
     * @return Paginated list of categories
     */
    @Transactional(readOnly = true)
    public Page<CategoryDto> getCategories(Pageable pageable, String searchQuery) {
        Page<Category> categoryPage;
        
        // If search query is provided, use search method, otherwise get all categories
        if (StringUtils.hasText(searchQuery)) {
            categoryPage = categoryRepository.searchCategories(searchQuery, pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        
        // Get product counts for each category
        Map<Integer, Long> productCountMap = getProductCountMap();
        
        // Map entities to DTOs with product counts
        return categoryPage.map(category -> {
            CategoryDto dto = ModelMapper.mapToCategoryDto(category);
            dto.setProductCount(productCountMap.getOrDefault(category.getId(), 0L));
            return dto;
        });
    }

    /**
     * Reads the details of a single category, identified by its id.
     * If not found, throws a custom exception (which can be mapped to HTTP 404).
     * Converts the entity to DTO for external representation.
     *
     * @param id database identifier of the category to fetch
     * @return CategoryDto representing the category
     * @throws ResourceNotFoundException if the category doesn't exist
     */
    public CategoryDto getCategoryById(Integer id) {
        // Attempt to fetch the category from the repository
        Category category = categoryRepository.findById(id)
                // If absent, throw helpful exception
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Get product count for this category
        Map<Integer, Long> productCountMap = getProductCountMap();
        
        // Map result to DTO with product count
        CategoryDto dto = ModelMapper.mapToCategoryDto(category);
        dto.setProductCount(productCountMap.getOrDefault(category.getId(), 0L));
        return dto;
    }

    /**
     * Retrieves the raw entity (not DTO) by ID.
     * Useful for internal logic or downstream operations that require the entity (not a DTO).
     *
     * @param id database ID of the category
     * @return the Category entity instance
     * @throws ResourceNotFoundException if not found
     */
    public Category getCategoryEntityById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    /**
     * Retrieves the raw entity (not DTO) by name.
     * Useful for internal logic or downstream operations that require the entity.
     *
     * @param name Name of the category
     * @return an Optional containing the Category entity instance, or empty if not found
     */
    public Optional<Category> getCategoryEntityByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name);
    }

    /**
     * Creates a new category from information in a DTO,
     * saves it to the DB, and then maps it back to a DTO to return with DB-generated fields included (like id).
     *
     * @param categoryDto DTO carrying the new category's data
     * @return DTO corresponding to the saved category
     * @throws ResourceAlreadyExistsException if a category with the same name already exists
     */
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        // Check if a category with the same name already exists (case-insensitive)
        if (categoryRepository.findByNameIgnoreCase(categoryDto.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        // Manually map DTO properties to a new entity object
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());

        // Save the entity (database assigns an ID, handles persistence)
        Category savedCategory = categoryRepository.save(category);
        
        // Map to DTO for API response or further processing
        CategoryDto savedDto = ModelMapper.mapToCategoryDto(savedCategory);
        savedDto.setProductCount(0L); // New category has no products
        return savedDto;
    }

    /**
     * Updates an existing category using new data from a DTO.
     * Fetches the entity, updates its fields, saves and maps it to a DTO.
     *
     * @param id ID of the category to update
     * @param categoryDto DTO with new field values
     * @return updated category as DTO
     * @throws ResourceNotFoundException if entity not found
     * @throws ResourceAlreadyExistsException if a category with the same name already exists (excluding this one)
     */
    @Transactional
    public CategoryDto updateCategory(Integer id, CategoryDto categoryDto) {
        // Load existing entity or throw if not found
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if another category with the same name already exists (case-insensitive, excluding this one)
        if (!category.getName().equalsIgnoreCase(categoryDto.getName()) && 
            categoryRepository.existsByNameIgnoreCaseAndIdNot(categoryDto.getName(), id)) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        // Update the mutable fields
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());

        // Persist changes
        Category updatedCategory = categoryRepository.save(category);
        
        // Get product count for this category
        Map<Integer, Long> productCountMap = getProductCountMap();
        
        // Convert to DTO for API return with product count
        CategoryDto updatedDto = ModelMapper.mapToCategoryDto(updatedCategory);
        updatedDto.setProductCount(productCountMap.getOrDefault(updatedCategory.getId(), 0L));
        return updatedDto;
    }

    /**
     * Deletes a category by its ID.
     * Checks for existence first, throwing exception if not present.
     *
     * @param id ID of category to delete
     * @throws ResourceNotFoundException if entity does not exist
     */
    @Transactional
    public void deleteCategory(Integer id) {
        // Get the category to check if it has products
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Check if the category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated products. Remove or reassign products first.");
        }
        
        // Remove from DB
        categoryRepository.deleteById(id);
    }
    
    /**
     * Helper method to get product counts for all categories
     * @return Map of category ID to product count
     */
    private Map<Integer, Long> getProductCountMap() {
        List<Object[]> productCounts = categoryRepository.countProductsByCategory();
        Map<Integer, Long> productCountMap = new HashMap<>();
        
        for (Object[] result : productCounts) {
            Integer categoryId = (Integer) result[0];
            Long count = (Long) result[1];
            productCountMap.put(categoryId, count);
        }
        
        return productCountMap;
    }
}
