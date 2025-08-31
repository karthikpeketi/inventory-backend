package com.inventory.repository;

import com.inventory.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository – Spring Data JPA repository for Category entities.
 *
 * Core concepts for learners:
 * - This interface declares database operations for Category objects.
 * - By extending JpaRepository, you get CRUD, pagination, and advanced Spring Data JPA support for free.
 * - No need to write implementation (Spring automatically provides it at runtime by proxy).
 * - @Repository marks it for component scanning, exception translation, and wiring.
 *
 * Custom Query Methods:
 * - Any method name in the pattern "findBy...", "existsBy...", "countBy...", etc., will be parsed by Spring
 *   and auto-implemented as a query based on entity fields.
 * - "existsByName" checks if a category name is present in the database (used for validation).
 *
 * Key inheritance:
 * - JpaRepository<Category, Integer> means: this manages Category entities, and their primary keys are Integer.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Derived Query Method: existsByName
     * 
     * Checks if a category with the given name exists in the database.
     * Spring Data JPA auto-implements this by parsing the method name and generating SQL accordingly.
     *
     * Usage example (in service layer): if (categoryRepository.existsByName("Electronics")) { ... }
     *
     * @param name The category name to test existence for.
     * @return true if a Category with given name exists, otherwise false.
     */
    boolean existsByName(String name);
    
    /**
     * Check if a category with the given name exists (case-insensitive), excluding the category with the given ID
     * @param name The category name to check
     * @param id The category ID to exclude
     * @return true if a category with the name exists (excluding the one with the given ID), false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
           "WHERE LOWER(c.name) = LOWER(:name) AND c.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Integer id);

    /**
     * Find a category by its name (case-insensitive)
     * @param name The category name to search for
     * @return Optional containing the category if found
     */
    Optional<Category> findByName(String name);
    
    /**
     * Find a category by its name (case-insensitive)
     * @param name The category name to search for
     * @return Optional containing the category if found
     */
    Optional<Category> findByNameIgnoreCase(String name);
    
    /**
     * Find all categories sorted by name
     * @return List of all categories sorted by name
     */
    List<Category> findAllByOrderByNameAsc();
    
    /**
     * Search for categories by name or description containing the search term (case-insensitive)
     * @param searchTerm The search term to look for
     * @param pageable Pagination information
     * @return Page of categories matching the search term
     */
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Category> searchCategories(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Count the number of products in each category
     * @return List of categories with product counts
     */
    @Query("SELECT c.id, COUNT(p) FROM Category c LEFT JOIN c.products p GROUP BY c.id")
    List<Object[]> countProductsByCategory();

    // Search methods for global search functionality
    List<Category> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);
}
