package com.inventory.controller;

import com.inventory.dto.InventoryTransactionDto;
import com.inventory.dto.ProductDto;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportsController
 * Provides endpoints for generating various business reports and analytics.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;
    
    @Autowired
    private InventoryService inventoryService;
    

    /**
     * Get stock movement data for a specified date range
     * @param startDate Start date in ISO format (yyyy-MM-dd)
     * @param endDate End date in ISO format (yyyy-MM-dd)
     * @return List of daily stock movement data
     */
    @GetMapping("/stock-movement")
    public ResponseEntity<List<Map<String, Object>>> getStockMovementData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // Default to last 30 days if dates not provided
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);
        
        // Get all transactions in the date range
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionsByDateRange(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        
        // Group transactions by date and type
        Map<LocalDate, Map<String, Integer>> movementByDate = new HashMap<>();
        
        for (InventoryTransactionDto tx : transactions) {
            LocalDate txDate = tx.getTransactionDate().toLocalDate();
            String type = tx.getTransactionType();
            
            movementByDate.putIfAbsent(txDate, new HashMap<>());
            Map<String, Integer> dailyMovement = movementByDate.get(txDate);
            
            if ("STOCK_IN".equals(type)) {
                dailyMovement.put("stockIn", dailyMovement.getOrDefault("stockIn", 0) + tx.getQuantity());
            } else if ("STOCK_OUT".equals(type)) {
                dailyMovement.put("stockOut", dailyMovement.getOrDefault("stockOut", 0) + tx.getQuantity());
            }
        }
        
        // Convert to list of maps for the response
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        
        // Ensure all dates in range are included
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date.format(formatter));
            
            Map<String, Integer> movements = movementByDate.getOrDefault(date, new HashMap<>());
            entry.put("stockIn", movements.getOrDefault("stockIn", 0));
            entry.put("stockOut", movements.getOrDefault("stockOut", 0));
            
            result.add(entry);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get top selling products for a specified period
     * @param period Period in days (default 30)
     * @param limit Number of products to return (default 5)
     * @return List of top selling products with sales quantities
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopSellingProducts(
            @RequestParam(defaultValue = "30") int period,
            @RequestParam(defaultValue = "5") int limit) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(period);
        
        // Get all STOCK_OUT transactions in the period
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionsByTypeAndDateRange(
                "STOCK_OUT", startDate, LocalDateTime.now());
        
        // Group by product and sum quantities
        Map<Integer, Map<String, Object>> productSales = new HashMap<>();
        
        for (InventoryTransactionDto tx : transactions) {
            if (tx.getProductId() == null) continue;
            
            Integer productId = tx.getProductId();
            String productName = tx.getProductName();
            
            if (!productSales.containsKey(productId)) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", productId);
                productData.put("name", productName);
                productData.put("sales", 0);
                productSales.put(productId, productData);
            }
            
            Map<String, Object> productData = productSales.get(productId);
            int currentSales = (int) productData.get("sales");
            productData.put("sales", currentSales + tx.getQuantity());
        }
        
        // Sort by sales and limit results
        List<Map<String, Object>> result = productSales.values().stream()
                .sorted((p1, p2) -> Integer.compare((int) p2.get("sales"), (int) p1.get("sales")))
                .limit(limit)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get slow moving products (products with low sales)
     * @param period Period in days (default 30)
     * @param limit Number of products to return (default 5)
     * @return List of slow moving products
     */
    @GetMapping("/slow-moving")
    public ResponseEntity<List<Map<String, Object>>> getSlowMovingProducts(
            @RequestParam(defaultValue = "30") int period,
            @RequestParam(defaultValue = "5") int limit) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(period);
        
        // Get all products
        List<ProductDto> allProducts = productService.getAllProducts();
        
        // Get all STOCK_OUT transactions in the period
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionsByTypeAndDateRange(
                "STOCK_OUT", startDate, LocalDateTime.now());
        
        // Calculate sales for each product
        Map<Integer, Integer> productSales = new HashMap<>();
        for (InventoryTransactionDto tx : transactions) {
            if (tx.getProductId() == null) continue;
            productSales.put(tx.getProductId(), 
                    productSales.getOrDefault(tx.getProductId(), 0) + tx.getQuantity());
        }
        
        // Find products with low or no sales
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductDto product : allProducts) {
            if (product.getQuantity() > 0) { // Only consider products with stock
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", product.getId());
                productData.put("name", product.getName());
                productData.put("sku", product.getSku());
                productData.put("currentStock", product.getQuantity());
                productData.put("sales", productSales.getOrDefault(product.getId(), 0));
                productData.put("daysInStock", period); // Simplified - assuming all products were in stock for the whole period
                
                result.add(productData);
            }
        }
        
        // Sort by sales (ascending) and limit results
        result.sort(Comparator.comparingInt(p -> (int) p.get("sales")));
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get supplier contribution data
     * @param period Period in days (default 90)
     * @return List of suppliers with their contribution percentages
     */
    @GetMapping("/supplier-contribution")
    public ResponseEntity<List<Map<String, Object>>> getSupplierContribution(
            @RequestParam(defaultValue = "90") int period) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(period);
        
        // Get all STOCK_IN transactions in the period
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionsByTypeAndDateRange(
                "STOCK_IN", startDate, LocalDateTime.now());
        
        // Group by supplier and calculate totals
        Map<Integer, Map<String, Object>> supplierData = new HashMap<>();
        double totalValue = 0.0;
        
        // Since we don't have direct supplier information in the ProductDto,
        // we'll use a simplified approach based on product IDs
        for (InventoryTransactionDto tx : transactions) {
            if (tx.getProductId() == null) continue;
            
            // Use product ID as supplier ID for demonstration
            // In a real implementation, you would need to fetch the supplier information
            // from a proper relationship between products and suppliers
            Integer productId = tx.getProductId();
            String productName = tx.getProductName();
            
            // Create a pseudo supplier ID and name based on the product
            Integer supplierId = productId;
            String supplierName = "Supplier for " + productName;
            double itemValue = tx.getQuantity() * (tx.getUnitPrice() != null ? tx.getUnitPrice() : 0.0);
            
            if (!supplierData.containsKey(supplierId)) {
                Map<String, Object> supplier = new HashMap<>();
                supplier.put("id", supplierId);
                supplier.put("name", supplierName);
                supplier.put("value", 0.0);
                supplierData.put(supplierId, supplier);
            }
            
            Map<String, Object> supplier = supplierData.get(supplierId);
            double currentValue = (double) supplier.get("value");
            supplier.put("value", currentValue + itemValue);
            
            totalValue += itemValue;
        }
        
        // Calculate percentages
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> supplier : supplierData.values()) {
            double value = (double) supplier.get("value");
            double percentage = totalValue > 0 ? (value / totalValue) * 100 : 0;
            supplier.put("percentage", Math.round(percentage * 100) / 100.0); // Round to 2 decimal places
            result.add(supplier);
        }
        
        // Sort by value descending
        result.sort((s1, s2) -> Double.compare((double) s2.get("value"), (double) s1.get("value")));
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get order value trend data
     * @param months Number of months to include (default 6)
     * @return Monthly order value data
     */
    @GetMapping("/order-value-trend")
    public ResponseEntity<List<Map<String, Object>>> getOrderValueTrend(
            @RequestParam(defaultValue = "6") int months) {
        
        // Reuse the existing monthly sales data method
        List<Map<String, Object>> salesData = purchaseOrderService.getMonthlySalesData(months);
        
        return ResponseEntity.ok(salesData);
    }
}