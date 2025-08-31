package com.inventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.inventory.dto.ProductDto;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Category;
import com.inventory.model.Product;
import com.inventory.model.User;
import com.inventory.model.InventoryTransaction;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.util.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * ProductService
 *
 * <p>
 * Centralizes all business logic for Product CRUD (Create, Read, Update, Delete) and product-related
 * queries. This includes searching, pagination, stock checks, and association with categories.
 * DTOs (Data Transfer Objects) are used for API communication, and active/inactive status
 * supports soft deletes.
 * </p>
 */
@Service // Makes this class available for dependency injection throughout Spring
public class ProductService {
    
    // Core repository for products and cross-service category lookup
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    
    // Valid sortable fields to prevent SQL injection
    private static final Set<String> VALID_SORT_FIELDS = Set.of(
        "name", "sku", "unitPrice", "quantity", "costPrice", "reorderLevel", "category.name"
    );
    
    /**
     * Constructor for injecting dependencies.
     * Spring will provide the correct implementations at runtime.
     */
    public ProductService(ProductRepository productRepository, CategoryService categoryService, 
                         UserRepository userRepository, InventoryTransactionRepository inventoryTransactionRepository) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }
    
    /**
     * List all products that are currently marked as active in the database.
     * Uses a custom repository method to ensure only non-deleted products are shown.
     * Optimized with modern Java stream operations for better performance.
     *
     * @return List of ProductDto for client use
     */
    public List<ProductDto> getAllProducts() {
        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        
        // For large datasets, consider using parallel streams
        // Use .parallelStream() if dealing with thousands of products
        return activeProducts.stream()
                .map(ModelMapper::mapToProductDto)
                .toList(); // Java 16+ feature, more efficient than collect(Collectors.toList())
    }
    
    /**
     * List all products that are currently marked as active in the database with sorting.
     * Uses Spring Data Sort to handle server-side sorting for better performance.
     *
     * @param sortBy Field to sort by (must be in VALID_SORT_FIELDS)
     * @param sortDirection Direction to sort (asc or desc)
     * @return List of ProductDto for client use, sorted as requested
     */
    public List<ProductDto> getAllProducts(String sortBy, String sortDirection) {
        // If no sorting parameters provided, use default method
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return getAllProducts();
        }
        
        // Validate sort field to prevent SQL injection
        if (!VALID_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy + 
                ". Valid fields are: " + VALID_SORT_FIELDS);
        }
        
        // Validate sort direction
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sort direction: " + sortDirection + 
                ". Valid directions are: asc, desc");
        }
        
        // Create Sort object and fetch sorted data (supports joined fields like category.name)
        Sort sort = Sort.by(direction, sortBy);
        List<Product> activeProducts = productRepository.findByIsActiveTrue(sort);
        
        return activeProducts.stream()
                .map(ModelMapper::mapToProductDto)
                .toList();
    }
    
    /**
     * Find a single active product by its database ID.
     * If not active or not found, throws an exception for proper API error handling.
     *
     * @param id The product's unique ID
     * @return ProductDto mapped for API or UI
     * @throws ResourceNotFoundException if product is not found or is not active
     */
    public ProductDto getProductById(Integer id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ModelMapper.mapToProductDto(product);
    }
    
    /**
     * Create a new product with information from a transfer object (DTO).
     * Assigns values, links the product to a category using categoryService,
     * and marks it as active.
     * Also creates an inventory transaction record for the initial stock.
     * @param productDto data for the new product
     * @param userId ID of the user creating the product
     * @return ProductDto after creation (including generated fields like ID)
     */
    @Transactional
    public ProductDto createProduct(ProductDto productDto, Integer userId) {
        // Default reorder level if not provided
        final Integer DEFAULT_REORDER_LEVEL = 10;
        
        // Create a blank entity and populate with DTO fields
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setBarcode(null); // Set barcode as null as per requirement
        // Resolve and assign category entity from categoryId in DTO
        product.setCategory(categoryService.getCategoryEntityById(productDto.getCategoryId()));
        product.setQuantity(productDto.getQuantity());
        // Set default reorder level if not provided
        product.setReorderLevel(productDto.getReorderLevel() != null ? productDto.getReorderLevel() : DEFAULT_REORDER_LEVEL);
        product.setUnitPrice(productDto.getUnitPrice());
        product.setCostPrice(productDto.getCostPrice());
        product.setImageUrl(productDto.getImageUrl());
        product.setIsActive(true); // Mark brand-new products as active
        
        // Save the newly created product entity
        Product savedProduct = productRepository.save(product);
        
        // Create inventory transaction for initial stock if quantity > 0
        if (productDto.getQuantity() != null && productDto.getQuantity() > 0) {
            createInitialStockTransaction(savedProduct, userId);
        }
        
        return ModelMapper.mapToProductDto(savedProduct);
    }
    
    /**
     * Update an existing product's details using new information from a DTO.
     * If the product is not found, throws a not found exception. Does not alter quantity or status.
     *
     * @param id Which product to update
     * @param productDto new info for the product
     * @return DTO of updated product
     */
    @Transactional
    public ProductDto updateProduct(Integer id, ProductDto productDto) {
        // Find the entity or throw not found
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check for quantity change (ADMIN only)
        Integer oldQuantity = product.getQuantity();
        Integer newQuantity = productDto.getQuantity();
        boolean quantityChanged = (newQuantity != null && !newQuantity.equals(oldQuantity));

        // Update mutable fields
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setBarcode(productDto.getBarcode());
        product.setCategory(categoryService.getCategoryEntityById(productDto.getCategoryId()));
        product.setReorderLevel(productDto.getReorderLevel());
        product.setUnitPrice(productDto.getUnitPrice());
        product.setCostPrice(productDto.getCostPrice());
        product.setImageUrl(productDto.getImageUrl());

        if (quantityChanged) {
            product.setQuantity(newQuantity);
        }

        // Update isActive if present in DTO
        if (productDto.getIsActive() != null) {
            product.setIsActive(productDto.getIsActive());
        }

        // Save the product first
        Product updatedProduct = productRepository.save(product);

        // Log ADJUSTMENT transaction if quantity changed
        if (quantityChanged) {
            // Get current authenticated user (ADMIN)
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(product);
            transaction.setUser(user);
            transaction.setTransactionType("ADJUSTMENT");
            transaction.setQuantity(Math.abs(newQuantity - oldQuantity));
            transaction.setNotes("Admin quantity adjustment (was: " + oldQuantity + ", now: " + newQuantity + ")");
            transaction.setReferenceNumber("PRODUCT_EDIT:" + product.getId());
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            inventoryTransactionRepository.save(transaction);
        }

        return ModelMapper.mapToProductDto(updatedProduct);
    }
    
    /**
     * Soft-deletes a product: marks as inactive instead of physically removing from DB.
     * This approach maintains record history and referential integrity.
     *
     * @param id The product's unique ID
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public void deleteProduct(Integer id) {
        // Retrieve entity or throw if missing
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        // Instead of delete, just mark isActive to false
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    /**
     * Search active products by partial (case-insensitive) name match.
     * Uses a custom repository query for search.
     *
     * @param name Part or all of product name
     * @return List of DTOs matching the search
     */
    public List<ProductDto> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name)
                .stream()
                .map(ModelMapper::mapToProductDto)
                .toList();
    }
    
    /**
     * List products that are below their assigned reorder threshold.
     * Uses repository-defined custom query to identify "low stock" products.
     *
     * @return List of low-stock product DTOs
     */
    public List<ProductDto> getLowStockProducts() {
        return productRepository.findLowStockProducts()
                .stream()
                .map(ModelMapper::mapToProductDto)
                .toList();
    }
    
    /**
     * Count how many products are currently marked as active.
     * Useful for dashboards, reporting, or monitoring.
     *
     * @return number of active products
     */
    public long getActiveProductCount() {
        return productRepository.countByIsActiveTrue();
    }
    
    /**
     * Returns a paginated list of products (for page-by-page API usage).
     * Each page will only contain active products, sorted according to passed parameters.
     *
     * @param search Search query
     * @param categoryId Category ID for filtering (can be null or "All")
     * @param sortBy Field to sort by (must be in VALID_SORT_FIELDS)
     * @param sortDirection Direction to sort (asc or desc)
     * @param pageable constructed by the controller or frontend (page, size only)
     * @return page of product DTOs
     */
    public Page<ProductDto> getPaginatedProducts(String search, String categoryId, String sortBy, String sortDirection, Pageable pageable) {
        Page<Product> productsPage;
        
        // Handle sorting
        Pageable sortedPageable = pageable;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            // Validate sort field to prevent SQL injection
            if (!VALID_SORT_FIELDS.contains(sortBy)) {
                throw new IllegalArgumentException("Invalid sort field: " + sortBy + 
                    ". Valid fields are: " + VALID_SORT_FIELDS);
            }
            
            // Validate sort direction
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(sortDirection);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid sort direction: " + sortDirection + 
                    ". Valid directions are: asc, desc");
            }
            
            // Create new Pageable with sorting
            Sort sort = Sort.by(direction, sortBy);
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
        }
        
        // Parse categoryId to Integer if it's not null and not "All"
        Integer categoryIdInt = null;
        if (categoryId != null && !categoryId.equalsIgnoreCase("All") && !categoryId.trim().isEmpty()) {
            try {
                categoryIdInt = Integer.parseInt(categoryId);
            } catch (NumberFormatException e) {
                throw new ResourceNotFoundException("Invalid category ID format: " + categoryId);
            }
        }

        if (search != null && !search.isBlank() && categoryIdInt != null) {
            // Search by both name/SKU and category ID
            Category category = categoryService.getCategoryEntityById(categoryIdInt);
            if (category == null) {
                throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
            }
            productsPage = productRepository.findByNameContainingIgnoreCaseAndCategoryAndIsActiveTrue(
                    search, category, sortedPageable);
        } else if (search != null && !search.isBlank()) {
            // Search by name or SKU only
            productsPage = productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseAndIsActiveTrue(
                    search, search, sortedPageable);
        } else if (categoryIdInt != null) {
            // Filter by category ID only
            Category category = categoryService.getCategoryEntityById(categoryIdInt);
            if (category == null) {
                throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
            }
            productsPage = productRepository.findByCategoryAndIsActiveTrue(category, sortedPageable);
        } else {
            // No search or category filter, just pagination
            productsPage = productRepository.findByIsActiveTrue(sortedPageable);
        }

        return productsPage.map(ModelMapper::mapToProductDto);
    }

    /**
     * Creates an initial stock transaction when a new product is added.
     * This records the initial inventory as a STOCK_IN transaction.
     *
     * @param product The newly created product
     * @param userId ID of the user creating the product
     */
    private void createInitialStockTransaction(Product product, Integer userId) {
        // Find the user who is creating the product
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Create the inventory transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setUser(user);
        transaction.setTransactionType("STOCK_IN");
        transaction.setQuantity(product.getQuantity());
        transaction.setReferenceNumber("NEW_PRODUCT:" + product.getId());
        transaction.setNotes("Initial stock for new product");
        transaction.setTransactionDate(LocalDateTime.now());
        
        // Save the transaction
        inventoryTransactionRepository.save(transaction);
    }
}