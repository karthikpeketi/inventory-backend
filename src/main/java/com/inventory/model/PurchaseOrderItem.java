package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "purchase_order_items")
@Data
public class PurchaseOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private Integer quantity;
    
    // @Column(nullable = false, precision = 10, scale = 2)
    @Column(columnDefinition = "float")
    private Double unitPrice;
    
    @Column(nullable = false)
    private Integer receivedQuantity = 0;
    
    private String notes;
}