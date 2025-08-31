package com.inventory.service;

import com.inventory.dto.*;
import com.inventory.model.*;
import com.inventory.repository.*;
import com.inventory.util.ModelMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public SearchResultDto globalSearch(String query, int limit) {
        SearchResultDto results = new SearchResultDto();
        
        results.setProducts(searchProducts(query, limit));
        results.setOrders(searchOrders(query, limit));
        results.setSuppliers(searchSuppliers(query, limit));
        results.setCategories(searchCategories(query, limit));
        results.setUsers(searchUsers(query, limit));
        
        return results;
    }

    public SearchResultDto globalSearch(String query, int limit, String entityType, 
                                      String dateRange, String status, String category,
                                      Double minPrice, Double maxPrice, String stockLevel) {
        SearchResultDto results = new SearchResultDto();
        
        // If entityType is specified, only search that entity type
        if ("products".equals(entityType)) {
            results.setProducts(searchProductsWithFilters(query, limit, category, minPrice, maxPrice, stockLevel));
        } else if ("orders".equals(entityType)) {
            results.setOrders(searchOrdersWithFilters(query, limit, status, dateRange));
        } else if ("suppliers".equals(entityType)) {
            results.setSuppliers(searchSuppliers(query, limit));
        } else if ("categories".equals(entityType)) {
            results.setCategories(searchCategories(query, limit));
        } else if ("users".equals(entityType)) {
            results.setUsers(searchUsers(query, limit));
        } else {
            // Search all entities (default behavior)
            results.setProducts(searchProductsWithFilters(query, limit, category, minPrice, maxPrice, stockLevel));
            results.setOrders(searchOrdersWithFilters(query, limit, status, dateRange));
            results.setSuppliers(searchSuppliers(query, limit));
            results.setCategories(searchCategories(query, limit));
            results.setUsers(searchUsers(query, limit));
        }
        
        return results;
    }

    public List<ProductDto> searchProducts(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        // Only return active products in global search results
        List<Product> products = productRepository.findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndSkuContainingIgnoreCase(
            query, query, pageable);
        
        return products.stream()
                .map(ModelMapper::mapToProductDto)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrderDto> searchOrders(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<PurchaseOrder> orders = purchaseOrderRepository.findByOrderNumberContainingIgnoreCaseOrCreatedByNameContainingIgnoreCase(
            query, query, pageable);
        
        return orders.stream()
                .map(this::mapToPurchaseOrderDto)
                .collect(Collectors.toList());
    }

    public List<SupplierDto> searchSuppliers(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        // Only return active suppliers in global search results
        List<Supplier> suppliers = supplierRepository.findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndContactPersonContainingIgnoreCaseOrIsActiveTrueAndEmailContainingIgnoreCase(
            query, query, query, pageable);
        
        return suppliers.stream()
                .map(this::mapToSupplierDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> searchCategories(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            query, query, pageable);
        
        return categories.stream()
                .map(this::mapToCategoryDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> searchUsers(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        // Only return active users in global search results
        List<User> users = userRepository.findByIsActiveTrueAndFirstNameContainingIgnoreCaseOrIsActiveTrueAndLastNameContainingIgnoreCaseOrIsActiveTrueAndEmailContainingIgnoreCase(
            query, query, query, pageable);
        
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    // Filtered search methods
    public List<ProductDto> searchProductsWithFilters(String query, int limit, String category, 
                                                     Double minPrice, Double maxPrice, String stockLevel) {
        Pageable pageable = PageRequest.of(0, limit);
        // Only return active products in global search results
        List<Product> products = productRepository.findByIsActiveTrueAndNameContainingIgnoreCaseOrIsActiveTrueAndSkuContainingIgnoreCase(
            query, query, pageable);
        
        // Apply filters
        return products.stream()
                .filter(product -> category == null || category.isEmpty() || 
                    (product.getCategory() != null && product.getCategory().getName().equalsIgnoreCase(category)))
                .filter(product -> minPrice == null || product.getUnitPrice() >= minPrice)
                .filter(product -> maxPrice == null || product.getUnitPrice() <= maxPrice)
                .filter(product -> stockLevel == null || stockLevel.isEmpty() || 
                    filterByStockLevel(product, stockLevel))
                .map(ModelMapper::mapToProductDto)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrderDto> searchOrdersWithFilters(String query, int limit, String status, String dateRange) {
        Pageable pageable = PageRequest.of(0, limit);
        List<PurchaseOrder> orders = purchaseOrderRepository.findByOrderNumberContainingIgnoreCaseOrCreatedByNameContainingIgnoreCase(
            query, query, pageable);
        
        // Apply filters
        return orders.stream()
                .filter(order -> status == null || status.isEmpty() || 
                    (order.getStatus() != null && order.getStatus().equalsIgnoreCase(status)))
                .filter(order -> dateRange == null || dateRange.isEmpty() || 
                    filterByDateRange(order, dateRange))
                .map(this::mapToPurchaseOrderDto)
                .collect(Collectors.toList());
    }

    private boolean filterByStockLevel(Product product, String stockLevel) {
        int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
        int reorderLevel = product.getReorderLevel() != null ? product.getReorderLevel() : 0;
        
        switch (stockLevel.toLowerCase()) {
            case "low":
                return quantity <= reorderLevel;
            case "normal":
                return quantity > reorderLevel && quantity <= reorderLevel * 3;
            case "high":
                return quantity > reorderLevel * 3;
            case "out":
                return quantity == 0;
            default:
                return true;
        }
    }

    private boolean filterByDateRange(PurchaseOrder order, String dateRange) {
        // For now, just return true. In a real implementation, you would
        // parse the dateRange and filter accordingly
        return true;
    }

    private PurchaseOrderDto mapToPurchaseOrderDto(PurchaseOrder order) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().toString() : null);
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedByName(order.getCreatedBy() != null ? 
            order.getCreatedBy().getFirstName() + " " + order.getCreatedBy().getLastName() : "");
        return dto;
    }

    private SupplierDto mapToSupplierDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setContactPerson(supplier.getContactPerson());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        return dto;
    }

    private CategoryDto mapToCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatusReason(user.getStatusReason());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}