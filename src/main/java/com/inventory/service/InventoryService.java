package com.inventory.service;

import com.inventory.dto.InventoryTransactionDto;
import com.inventory.dto.ProductDto;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.InventoryTransaction;
import com.inventory.model.Product;
import com.inventory.model.User;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.util.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.time.YearMonth;

/**
 * InventoryService
 *
 * <p>
 * This class encapsulates all core business logic for inventory management: stock in/out operations,
 * transaction and product queries, and calculation of inventory value. All sensitive
 * operations (stock changes) are done within transactions for data consistency.
 * </p>
 * <p>
 * By isolating inventory logic here, controllers and other layers remain clean and easy to maintain.
 * </p>
 */
@Service // Mark as a Spring-managed service bean for dependency injection
public class InventoryService {
    // Repositories provide access to persistent data
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Constructor for dependency injection (constructor-based is safest for immutability and testing).
     */
    public InventoryService(InventoryTransactionRepository transactionRepository,
                          ProductRepository productRepository,
                          UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * Sell Product Operation (sell product from inventory).
     *
     * <p>
     * This method allows both ADMIN and STAFF to sell products.
     * It updates product stock quantities and creates a sales transaction record.
     * </p>
     * 
     * @param productId The ID of the product to sell
     * @param quantity The quantity to sell
     * @param notes Optional notes about the sale
     * @return The created transaction DTO
     * @throws ResourceNotFoundException if product not found or insufficient stock
     */
    @Transactional
    public InventoryTransactionDto sellProduct(Integer productId, int quantity, String notes) {
        // Step 1: Find the product or throw exception if not found
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Step 2: Validate sufficient stock
        if (product.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock. Available: " + product.getQuantity());
        }

        // Step 3: Get the current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Step 4: Update product quantity
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        // Step 5: Create and save transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setUser(user);
        transaction.setTransactionType("STOCK_OUT");
        transaction.setQuantity(quantity);
        transaction.setNotes(notes);
        transaction.setTransactionDate(LocalDateTime.now());
        
        // Step 6: Save and return the transaction
        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        return ModelMapper.mapToInventoryTransactionDto(savedTransaction);
    }

    /**
     * Get a fixed number of the most recent transactions (across all products).
     * Useful for dashboards or notifications.
     *
     * @param count how many transactions to fetch (e.g., 5, 10, etc.)
     * @return most recent transaction DTOs
     */
    public List<InventoryTransactionDto> getRecentTransactions(int count) {
        // PageRequest for the first page of given size, ordered by date (desc)
        Pageable pageable = PageRequest.of(0, count, Sort.by("transactionDate").descending());
        return transactionRepository.findAll(pageable)
                .stream()
                .map(ModelMapper::mapToInventoryTransactionDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the total value of all the currently active products in inventory.
     * Value = quantity * unit cost price, only for active (not deleted) products.
     *
     * @return total inventory value as double
     */
    public double getInventoryValue() {
        // .filter to skip inactive, .mapToDouble computes sum by multiplying quantity and cost price
        return productRepository.findAll()
                .stream()
                .filter(p -> p.getIsActive())
                .mapToDouble(p -> p.getQuantity() * p.getCostPrice())
                .sum();
    }

    /**
     * Looks up a product in the inventory using its barcode, only if active.
     *
     * @param barcode product barcode string
     * @return product as DTO, or throws if not found
     */
    public ProductDto getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeAndIsActiveTrue(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
        return ModelMapper.mapToProductDto(product);
    }
    /**
     * Count all STOCK_OUT transactions for the current month (sold orders).
     */
    public int getTotalStockOutCountForCurrentMonth() {
        YearMonth now = YearMonth.now();
        LocalDateTime start = now.atDay(1).atStartOfDay();
        LocalDateTime end = now.plusMonths(1).atDay(1).atStartOfDay();
        return (int) transactionRepository.countByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
    }

    /**
     * Same for previous month (for delta calculation).
     */
    public int getTotalStockOutCountForPreviousMonth() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        LocalDateTime start = prev.atDay(1).atStartOfDay();
        LocalDateTime end = prev.plusMonths(1).atDay(1).atStartOfDay();
        return (int) transactionRepository.countByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
    }

    /**
     * Calculate total revenue from STOCK_OUT transactions for the current month.
     * Revenue is calculated as quantity * selling price for each transaction.
     * 
     * @return total revenue for current month
     */
    public double getRevenueForCurrentMonth() {
        YearMonth now = YearMonth.now();
        LocalDateTime start = now.atDay(1).atStartOfDay();
        LocalDateTime end = now.plusMonths(1).atDay(1).atStartOfDay();
        List<InventoryTransaction> txs = transactionRepository
            .findByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
        return txs.stream()
            .mapToDouble(tx -> {
                Double price = (tx.getProduct() != null) ? tx.getProduct().getUnitPrice() : 0.0;
                return tx.getQuantity() * (price != null ? price : 0.0);
            })
            .sum();
    }

    /**
     * Calculate total revenue from STOCK_OUT transactions for the previous month.
     * Used for month-over-month comparison on dashboard.
     * 
     * @return total revenue for previous month
     */
    public double getRevenueForPreviousMonth() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        LocalDateTime start = prev.atDay(1).atStartOfDay();
        LocalDateTime end = prev.plusMonths(1).atDay(1).atStartOfDay();
        List<InventoryTransaction> txs = transactionRepository
            .findByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
        return txs.stream()
            .mapToDouble(tx -> {
                Double price = (tx.getProduct() != null) ? tx.getProduct().getUnitPrice() : 0.0;
                return tx.getQuantity() * (price != null ? price : 0.0);
            })
            .sum();
    }

    /**
     * Get recent sale transactions (stock-outs), newest first, up to count.
     * Used for displaying recent sales on dashboard.
     * 
     * @param count number of transactions to return
     * @return list of recent stock out transaction DTOs
     */
    public List<InventoryTransactionDto> getRecentStockOutTransactions(int count) {
        return transactionRepository.findByTransactionTypeOrderByTransactionDateDesc("STOCK_OUT")
            .stream()
            .limit(count)
            .map(ModelMapper::mapToInventoryTransactionDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent sale transactions (stock-outs) with pagination support.
     * Used for displaying recent sales on dashboard with pagination.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy sort field
     * @param direction sort direction
     * @return list of recent stock out transaction DTOs
     */
    public List<InventoryTransactionDto> getRecentStockOutTransactions(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return transactionRepository.findByTransactionType("STOCK_OUT", pageable)
            .stream()
            .map(ModelMapper::mapToInventoryTransactionDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent sale transactions (stock-outs) with pagination support.
     * Used for displaying recent sales on dashboard with pagination.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy sort field
     * @return list of recent stock out transaction DTOs
     */
    public List<InventoryTransactionDto> getRecentStockOutTransactions(int page, int size, String sortBy) {
        return getRecentStockOutTransactions(page, size, sortBy, "desc");
    }
    
    /**
     * Get total count of stock out transactions.
     * Used for pagination.
     * 
     * @return total count of stock out transactions
     */
    public int getTotalStockOutCount() {
        return (int) transactionRepository.countByTransactionType("STOCK_OUT");
    }
    
    /**
     * Get transactions within a specific date range
     * 
     * @param startDate start date and time (inclusive)
     * @param endDate end date and time (exclusive)
     * @return list of transactions within the date range
     */
    public List<InventoryTransactionDto> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate)
                .stream()
                .map(ModelMapper::mapToInventoryTransactionDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get transactions of a specific type within a date range
     * 
     * @param transactionType type of transaction (e.g., STOCK_IN, STOCK_OUT)
     * @param startDate start date and time (inclusive)
     * @param endDate end date and time (exclusive)
     * @return list of transactions of the specified type within the date range
     */
    public List<InventoryTransactionDto> getTransactionsByTypeAndDateRange(
            String transactionType, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                transactionType, startDate, endDate)
                .stream()
                .map(ModelMapper::mapToInventoryTransactionDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Count all STOCK_OUT transactions for the last 30 days (for dashboard stats).
     * 
     * @return total count of stock out transactions in the last 30 days
     */
    public int getTotalStockOutCountForLast30Days() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        return (int) transactionRepository.countByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
    }
    
    /**
     * Count all STOCK_OUT transactions for the previous 30 days (31-60 days ago).
     * Used for trend calculation on dashboard.
     * 
     * @return total count of stock out transactions in the previous 30 days
     */
    public int getTotalStockOutCountForPrevious30Days() {
        LocalDateTime start = LocalDateTime.now().minusDays(60);
        LocalDateTime end = LocalDateTime.now().minusDays(30);
        return (int) transactionRepository.countByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
    }
    
    /**
     * Calculate total revenue from STOCK_OUT transactions for the last 30 days.
     * Revenue is calculated as quantity * selling price for each transaction.
     * 
     * @return total revenue for the last 30 days
     */
    public double getRevenueForLast30Days() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        List<InventoryTransaction> txs = transactionRepository
            .findByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
        return txs.stream()
            .mapToDouble(tx -> {
                Double price = (tx.getProduct() != null) ? tx.getProduct().getUnitPrice() : 0.0;
                return tx.getQuantity() * (price != null ? price : 0.0);
            })
            .sum();
    }
    
    /**
     * Calculate total revenue from STOCK_OUT transactions for the previous 30 days (31-60 days ago).
     * Used for month-over-month comparison on dashboard.
     * 
     * @return total revenue for the previous 30 days
     */
    public double getRevenueForPrevious30Days() {
        LocalDateTime start = LocalDateTime.now().minusDays(60);
        LocalDateTime end = LocalDateTime.now().minusDays(30);
        List<InventoryTransaction> txs = transactionRepository
            .findByTransactionTypeAndTransactionDateBetween("STOCK_OUT", start, end);
        return txs.stream()
            .mapToDouble(tx -> {
                Double price = (tx.getProduct() != null) ? tx.getProduct().getUnitPrice() : 0.0;
                return tx.getQuantity() * (price != null ? price : 0.0);
            })
            .sum();
    }
    
    /**
     * Get count of products with low stock for the previous 30 days.
     * This is a placeholder implementation - you may need to adjust based on your business logic.
     * 
     * @return count of low stock products for the previous 30 days
     */
    public int getLowStockCountForPrevious30Days() {
        // This is a simplified implementation. You may need to adjust based on your specific requirements.
        // For now, we'll return the current low stock count as a placeholder.
        // In a real scenario, you might want to track historical low stock data.
        return productRepository.findLowStockProducts().size();
    }
}
