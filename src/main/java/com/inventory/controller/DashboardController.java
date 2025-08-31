package com.inventory.controller;

import com.inventory.dto.InventoryTransactionDto;
import com.inventory.dto.ProductDto;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DashboardController
 * Aggregates key stats & summaries for dashboard display.
 * 
 * Note: The InventoryTransactionDto should include a sellingPrice field
 * that needs to be properly populated in the service layer.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;
    
    @Autowired
    private InventoryService inventoryService;

    // 1. Stats endpoint (products, orders, low stock count, revenue)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> res = new HashMap<>();
        
        // Current data (last 30 days)
        int totalProducts = productService.getAllProducts().size();
        int totalOrders = inventoryService.getTotalStockOutCountForLast30Days();
        int lowStockCount = productService.getLowStockProducts().size();
        double monthlyRevenue = inventoryService.getRevenueForLast30Days();
        
        // Previous period data for trend calculation (31-60 days ago)
        int prevTotalOrders = inventoryService.getTotalStockOutCountForPrevious30Days();
        double prevMonthlyRevenue = inventoryService.getRevenueForPrevious30Days();
        int prevLowStockCount = inventoryService.getLowStockCountForPrevious30Days();
        
        // Calculate trends
        double ordersChange = 0.0;
        boolean ordersIsPositive = true;
        if (prevTotalOrders > 0) {
            ordersChange = ((double) (totalOrders - prevTotalOrders) / prevTotalOrders) * 100;
            ordersIsPositive = ordersChange >= 0;
        } else if (totalOrders > 0) {
            ordersChange = 100.0; // New orders, so 100% increase
            ordersIsPositive = true;
        }
        
        double revenueChange = 0.0;
        boolean revenueIsPositive = true;
        if (prevMonthlyRevenue > 0) {
            revenueChange = ((monthlyRevenue - prevMonthlyRevenue) / prevMonthlyRevenue) * 100;
            revenueIsPositive = revenueChange >= 0;
        } else if (monthlyRevenue > 0) {
            revenueChange = 100.0; // New revenue, so 100% increase
            revenueIsPositive = true;
        }
        
        // Low stock change
        double lowStockChange = 0.0;
        boolean lowStockIsPositive = false;
        if (prevLowStockCount > 0) {
            lowStockChange = ((double) (lowStockCount - prevLowStockCount) / prevLowStockCount) * 100;
            lowStockIsPositive = lowStockChange <= 0; // Decreasing low stock is good
        } else if (lowStockCount > 0) {
            lowStockChange = 100.0;
            lowStockIsPositive = false; // New low stock items is bad
        }
        
        res.put("totalProducts", totalProducts);
        res.put("totalOrders", totalOrders);
        res.put("lowStockCount", lowStockCount);
        res.put("monthlyRevenue", monthlyRevenue);
        
        // Add trend data
        res.put("totalProductsChange", 0.0); // No historical data available
        res.put("totalProductsIsPositive", true);
        
        res.put("totalOrdersChange", Math.abs(ordersChange));
        res.put("totalOrdersIsPositive", ordersIsPositive);
        
        res.put("lowStockChange", Math.abs(lowStockChange));
        res.put("lowStockIsPositive", lowStockIsPositive);
        
        res.put("monthlyRevenueChange", Math.abs(revenueChange));
        res.put("monthlyRevenueIsPositive", revenueIsPositive);
        
        return ResponseEntity.ok(res);
    }

    // 2. Recent Orders
    @GetMapping("/recent-orders")
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders() {
        
        // Get recent orders (limited to 10 most recent)
        List<InventoryTransactionDto> txList = inventoryService.getRecentStockOutTransactions(0, 10, "transactionDate", "desc");
        
        List<Map<String, Object>> orders = txList.stream().map(tx -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", tx.getId());
            map.put("customer", tx.getUsername()); // Fixed from getUserName() to getUsername()
            map.put("date", tx.getTransactionDate());
            map.put("orderDate", tx.getTransactionDate()); // Add orderDate for compatibility
            // Handle potential null selling price
            map.put("total", tx.getQuantity() * (tx.getUnitPrice() != null ? tx.getUnitPrice() : 0.0));
            map.put("totalAmount", tx.getQuantity() * (tx.getUnitPrice() != null ? tx.getUnitPrice() : 0.0)); // Add totalAmount for compatibility
            map.put("status", "Delivered"); // Changed to match expected status
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }

    // 3. Low Stock Items
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockItems() {
        
        List<ProductDto> lowStockItems = productService.getLowStockProducts();
        
        return ResponseEntity.ok(lowStockItems);
    }

    // 4. Sales Data (bar chart - group by month)
    @GetMapping("/sales")
    public ResponseEntity<List<Map<String, Object>>> getSalesData(
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> sales = purchaseOrderService.getMonthlySalesData(months);
        return ResponseEntity.ok(sales);
    }
}
