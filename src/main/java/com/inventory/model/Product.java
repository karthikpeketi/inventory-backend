package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Data
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    private String sku;
    private String barcode;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(nullable = false)
    private Integer quantity = 0;
    
    @Column(nullable = false)
    private Integer reorderLevel = 5;
    
    // @Column(nullable = false, precision = 10, scale = 2)
    @Column(columnDefinition = "float")
    private double unitPrice;
    
    // @Column(nullable = false, precision = 10, scale = 2)
    @Column(columnDefinition = "float")
    private double costPrice;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "product")
    private List<InventoryTransaction> transactions;
    
    @OneToMany(mappedBy = "product")
    private List<PurchaseOrderItem> orderItems;
}