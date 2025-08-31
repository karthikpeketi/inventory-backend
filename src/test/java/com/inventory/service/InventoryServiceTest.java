package com.inventory.service;

import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class InventoryServiceTest {

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTotalStockOutCountForLast30Days() {
        // Arrange
        when(transactionRepository.countByTransactionTypeAndTransactionDateBetween(
            eq("STOCK_OUT"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(5L);

        // Act
        int result = inventoryService.getTotalStockOutCountForLast30Days();

        // Assert
        assertEquals(5, result);
    }

    @Test
    void testGetTotalStockOutCountForPrevious30Days() {
        // Arrange
        when(transactionRepository.countByTransactionTypeAndTransactionDateBetween(
            eq("STOCK_OUT"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(3L);

        // Act
        int result = inventoryService.getTotalStockOutCountForPrevious30Days();

        // Assert
        assertEquals(3, result);
    }

    @Test
    void testGetRevenueForLast30Days() {
        // Arrange
        when(transactionRepository.findByTransactionTypeAndTransactionDateBetween(
            eq("STOCK_OUT"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(new ArrayList<>()); // Empty list for simplicity

        // Act
        double result = inventoryService.getRevenueForLast30Days();

        // Assert
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testGetRevenueForPrevious30Days() {
        // Arrange
        when(transactionRepository.findByTransactionTypeAndTransactionDateBetween(
            eq("STOCK_OUT"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(new ArrayList<>()); // Empty list for simplicity

        // Act
        double result = inventoryService.getRevenueForPrevious30Days();

        // Assert
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testGetLowStockCountForPrevious30Days() {
        // Arrange
        when(productRepository.findLowStockProducts())
            .thenReturn(new ArrayList<>()); // Empty list for simplicity

        // Act
        int result = inventoryService.getLowStockCountForPrevious30Days();

        // Assert
        assertEquals(0, result);
    }
}