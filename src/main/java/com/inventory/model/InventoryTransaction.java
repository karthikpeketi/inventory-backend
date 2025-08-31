package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
public class InventoryTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String transactionType; // STOCK_IN, STOCK_OUT, ADJUSTMENT
    
    @Column(nullable = false)
    private Integer quantity;
    
    private String notes;
    private String referenceNumber;
    
    @Column(nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();
}