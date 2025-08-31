package com.inventory.service;

import com.inventory.dto.PurchaseOrderDto;
import com.inventory.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface PurchaseOrderService {
    // Enhanced operations with pagination and filtering
    Page<PurchaseOrderDto> getAllOrders(Pageable pageable, String status, String search, String sortBy, String sortDirection);
    PurchaseOrderDto getOrderById(Integer id);
    PurchaseOrderDto createOrder(PurchaseOrderDto dto, Integer userId);
    PurchaseOrderDto updateOrder(Integer id, PurchaseOrderDto dto, Integer userId, String userRole);
    PurchaseOrderDto updateOrderStatus(Integer id, String status, Integer userId, String userRole);
    void deleteOrder(Integer id);
    PurchaseOrderDto completeOrder(Integer id, Integer userId, String userRole);
    
    // Dashboard operations
    /** Returns ALL PurchaseOrder entities */
    List<PurchaseOrder> getAllPurchaseOrders();

    /** Returns the most recent N PurchaseOrder entities (ordered by date desc) */
    List<PurchaseOrder> getRecentOrders(int count);

    /** Returns total sales (revenue) just for orders in the current month */
    double getRevenueForCurrentMonth();

    /**
     * Returns list of {month, sales} for the last N months for chart display.
     * Each map: { "month": String ("Jan"), "sales": Double }
     */
    List<Map<String, Object>> getMonthlySalesData(int months);
}
