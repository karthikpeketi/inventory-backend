package com.inventory.repository;

import com.inventory.model.InventoryTransaction;

import org.springframework.data.domain.Pageable; // Fixed import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InventoryTransactionRepository – JPA repository for InventoryTransaction entities.
 * 
 * For learners:
 * - Extends JpaRepository to inherit standard CRUD operations (Create, Read, Update, Delete)
 *   and pagination capabilities for InventoryTransaction objects.
 * - Spring Data JPA automatically implements this interface at runtime.
 * - Method names following Spring Data naming conventions are automatically converted into SQL queries.
 * - Custom queries can be defined using the @Query annotation with either JPQL or native SQL.
 * - @Repository marks this interface as a Spring-managed repository bean.
 * 
 * Main functionality:
 * - Retrieve transactions by product
 * - Get all transactions in chronological order
 * - Find recent transactions
 * - Support for paginated queries
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    
    /**
     * Finds all transactions for a specific product, ordered by transaction date (newest first).
     * 
     * This is a "derived query method" where Spring Data JPA parses the method name
     * and automatically generates the appropriate JPQL query:
     * SELECT t FROM InventoryTransaction t WHERE t.productId = ?1 ORDER BY t.transactionDate DESC
     * 
     * Method name breakdown:
     * - findBy: Indicates a query that filters results
     * - ProductId: The entity field to filter on
     * - OrderByTransactionDateDesc: Sort results by transactionDate in descending order
     * 
     * @param productId The ID of the product to find transactions for
     * @return List of transactions for the specified product, newest first
     */
    List<InventoryTransaction> findByProductIdOrderByTransactionDateDesc(Integer productId);
    
    /**
     * Retrieves all inventory transactions across all products, ordered by date (newest first).
     * 
     * This is another derived query method where Spring generates:
     * SELECT t FROM InventoryTransaction t ORDER BY t.transactionDate DESC
     * 
     * @return Chronologically descending list of all transactions
     */
    List<InventoryTransaction> findAllByOrderByTransactionDateDesc();
    
    /**
     * Retrieves the 5 most recent inventory transactions using native SQL.
     * 
     * This demonstrates using a native SQL query with the @Query annotation:
     * - nativeQuery = true indicates this is raw SQL rather than JPQL
     * - Uses database table/column names instead of entity/field names
     * - LIMIT 5 is database-specific syntax (works in MySQL, PostgreSQL, etc.)
     * 
     * Note: Native queries are less portable across different database systems
     * but allow using database-specific features.
     * 
     * @return The 5 most recent InventoryTransaction records
     */
    @Query(value = "SELECT * FROM inventory_transactions ORDER BY transaction_date DESC LIMIT 5", nativeQuery = true)
    List<InventoryTransaction> findTop5RecentTransactions();

    /**
     * Retrieves transactions ordered by date with flexible pagination.
     * 
     * This demonstrates:
     * - Using @Query with JPQL (Java Persistence Query Language)
     * - Working with entity classes/fields instead of database tables/columns
     * - Supporting dynamic "top N" queries through the Pageable parameter
     * 
     * Usage example:
     *   // To get top 10 transactions:
     *   repository.findTopNByOrderByTransactionDateDesc(PageRequest.of(0, 10));
     * 
     * Note: This method should ideally use org.springframework.data.domain.Pageable
     * instead of org.springdoc.core.converters.models.Pageable for proper Spring Data integration.
     * 
     * @param pageable Controls pagination (page number, size, and optional sorting)
     * @return List of transactions limited by the pageable parameter
     */
    @Query("SELECT t FROM InventoryTransaction t ORDER BY t.transactionDate DESC")
    List<InventoryTransaction> findTopNByOrderByTransactionDateDesc(Pageable pageable);

    List<InventoryTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
        LocalDateTime from, LocalDateTime to);
        
    List<InventoryTransaction> findByTransactionTypeAndTransactionDateBetween(
        String transactionType, LocalDateTime from, LocalDateTime to);
        
    List<InventoryTransaction> findByTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
        String transactionType, LocalDateTime from, LocalDateTime to);

    // In InventoryTransactionRepository
    long countByTransactionTypeAndTransactionDateBetween(String type, LocalDateTime from, LocalDateTime to);
    List<InventoryTransaction> findByTransactionTypeOrderByTransactionDateDesc(String type);
    
    /**
     * Finds transactions for dashboard revenue calculations within a date range.
     * Useful for generating revenue reports and charts on the dashboard.
     * 
     * @param transactionType The type of transaction (typically "SALE")
     * @param startDate The start date for the report period
     * @param endDate The end date for the report period
     * @return List of transactions matching the criteria
     */
    List<InventoryTransaction> findByTransactionTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
            String transactionType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Calculates the total value of transactions of a specific type within a date range.
     * Particularly useful for dashboard KPIs showing total revenue or costs.
     * 
     * @param transactionType The type of transaction to sum (e.g., "SALE", "PURCHASE")
     * @param startDate The start date for the calculation period
     * @param endDate The end date for the calculation period
     * @return The sum of transaction values
     */
    @Query("SELECT SUM(t.quantity * t.product.unitPrice) FROM InventoryTransaction t WHERE t.transactionType = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    Double calculateTotalValueByTypeAndDateRange(String transactionType, LocalDateTime startDate, LocalDateTime endDate);
    /**
     * Retrieves daily transaction totals for a given period.
     * Perfect for time-series charts on dashboards.
     * 
     * @param transactionType The type of transaction to analyze
     * @param startDate The start date for the report
     * @param endDate The end date for the report
     * @return List of daily totals with date and amount
     */
    @Query("SELECT FUNCTION('DATE', t.transactionDate) as date, " +
           "SUM(t.quantity * t.product.unitPrice) as total " +
           "FROM InventoryTransaction t " +
           "WHERE t.transactionType = ?1 AND t.transactionDate BETWEEN ?2 AND ?3 " +
           "GROUP BY FUNCTION('DATE', t.transactionDate) " +
           "ORDER BY date ASC")
    List<Object[]> getDailyTransactionTotals(String transactionType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds the most recent sales transactions for dashboard display.
     * Provides a quick overview of recent business activity.
     * 
     * @param limit The maximum number of transactions to return
     * @return List of recent sales transactions
     */
    @Query(value = "SELECT * FROM inventory_transactions " +
                  "WHERE transaction_type = 'SALE' " +
                  "ORDER BY transaction_date DESC LIMIT ?1", nativeQuery = true)
    List<InventoryTransaction> findRecentSales(int limit);
    
    // Add methods for pagination support
    List<InventoryTransaction> findByTransactionType(String transactionType, Pageable pageable);
    long countByTransactionType(String transactionType);
}
