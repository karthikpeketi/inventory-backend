package com.inventory.repository;

import com.inventory.model.PurchaseOrder;
import com.inventory.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer>, JpaSpecificationExecutor<PurchaseOrder> {

    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.createdBy u WHERE " +
           "(:status IS NULL OR :status = '' OR po.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.status) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(po.orderDate AS string) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(po.expectedDeliveryDate AS string) LIKE CONCAT('%', :search, '%'))")
    Page<PurchaseOrder> findOrdersWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
            
    // Add custom queries for sorting by createdBy.username in both directions
    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.createdBy u WHERE " +
           "(:status IS NULL OR :status = '' OR po.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.status) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(po.orderDate AS string) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(po.expectedDeliveryDate AS string) LIKE CONCAT('%', :search, '%'))" +
           "ORDER BY u.username ASC")
    Page<PurchaseOrder> findOrdersWithFiltersSortByUsernameAsc(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
            
    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.createdBy u WHERE " +
           "(:status IS NULL OR :status = '' OR po.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.status) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(po.orderDate AS string) LIKE CONCAT('%', :search, '%') OR " +
           "CAST(po.expectedDeliveryDate AS string) LIKE CONCAT('%', :search, '%'))" +
           "ORDER BY u.username DESC")
    Page<PurchaseOrder> findOrdersWithFiltersSortByUsernameDesc(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
            
    /**
     * Find purchase orders for a specific supplier with the given statuses.
     * Used to check for active orders before supplier deletion.
     * 
     * @param supplier The supplier to check for active orders
     * @param statuses List of order statuses to check
     * @return List of purchase orders matching the criteria
     */
    List<PurchaseOrder> findBySupplierAndStatusIn(Supplier supplier, List<String> statuses);

    // Search methods for global search functionality
    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.createdBy u WHERE " +
           "LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :createdByName, '%'))")
    List<PurchaseOrder> findByOrderNumberContainingIgnoreCaseOrCreatedByNameContainingIgnoreCase(
            @Param("orderNumber") String orderNumber,
            @Param("createdByName") String createdByName,
            Pageable pageable);
}
