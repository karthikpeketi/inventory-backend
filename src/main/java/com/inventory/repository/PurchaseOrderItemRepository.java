package com.inventory.repository;

import com.inventory.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {
    
    /**
     * Delete all purchase order items for a specific order
     * 
     * @param orderId The ID of the order whose items should be deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PurchaseOrderItem poi WHERE poi.order.id = :orderId")
    void deleteByOrderId(Integer orderId);
    
    /**
     * Find all purchase order items for a specific order
     * 
     * @param orderId The ID of the order whose items should be retrieved
     * @return List of purchase order items for the specified order
     */
    @Query("SELECT poi FROM PurchaseOrderItem poi WHERE poi.order.id = :orderId")
    List<PurchaseOrderItem> findByOrderId(Integer orderId);
}