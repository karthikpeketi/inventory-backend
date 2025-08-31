package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PurchaseOrder – JPA entity representing a supplier order in the system.
 *
 * Concepts:
 * - Each instance represents a distinct order a business has placed with a supplier for inventory products.
 * - Captures supplier, financial, scheduling, and status information.
 * - Demonstrates JPA entity modeling with relations, auditing, status enums, and total calculations.
 * 
 * Lombok:
 * - @Data: reduces boilerplate by auto-generating getters, setters, equals, hashCode, and toString methods.
 * 
 * JPA Annotations:
 * - @Entity: Marks this class as a JPA entity to be persisted in the database.
 * - @Table: Specifies the database table name for this entity.
 *
 * Relationships:
 * - Many-to-one to Supplier (multiple orders per supplier)
 * - Many-to-one to User (who created the order)
 * - One-to-many to PurchaseOrderItem (each order may contain multiple line items)
 */
@Entity
@Table(name = "purchase_orders")
@Data
public class PurchaseOrder {
    
    /**
     * Primary key for the purchase order.
     * 
     * @Id - Marks this field as the primary key
     * @GeneratedValue - Configures auto-increment strategy for the primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    /**
     * The supplier from whom products are being ordered.
     * 
     * @ManyToOne - Multiple purchase orders can be associated with a single supplier
     * @JoinColumn - Specifies the foreign key column in the database
     *   - nullable=false: A purchase order must have a supplier
     */
    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    /**
     * Business-facing order reference number.
     * This is different from the database ID and is used in business communications.
     * 
     * @Column attributes:
     *   - nullable=false: Every order must have an order number
     *   - unique=true: No two orders can have the same order number
     */
    @Column(nullable = false, unique = true)
    private String orderNumber;
    
    /**
     * The date and time when the order was placed.
     * Default value is set to the current time when a new order is created.
     */
    @Column(nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();
    
    /**
     * The date and time when the order is expected to be Delivered.
     * This is optional and may be set after the order is created.
     */
    private LocalDateTime expectedDeliveryDate;
    
    /**
     * Current status of the purchase order.
     * Status workflow:
     * - PENDING: Order has been created but not yet approved
     * - PROCESSING: Order has been approved and is being processed
     * - DELIVERED: Order has been received and processed
     * - CANCELLED: Order has been cancelled
     */
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PROCESSING, DELIVERED, CANCELLED
    
    /**
     * The total monetary value of the order.
     * This is the sum of all line items' costs.
     * 
     * Note: For financial calculations in production systems,
     * BigDecimal would be preferred over double for precision.
     * 
     * The commented line shows an alternative precision-based approach.
     */
    // @Column(nullable = false, precision = 12, scale = 2)
    @Column(columnDefinition = "float")
    private double totalAmount;
    
    /**
     * Optional notes or special instructions for this order.
     */
    private String notes;
    
    /**
     * Reference to the user who created this purchase order.
     * 
     * @ManyToOne - A user can create multiple purchase orders
     * @JoinColumn - Specifies the foreign key column in the database
     */
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    /**
     * Timestamp of when this record was created in the database.
     * 
     * @CreationTimestamp - Hibernate will automatically set this value on insert
     * @Column(updatable=false) - Ensures this value cannot be changed after creation
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp of when this record was last updated.
     * 
     * @UpdateTimestamp - Hibernate will automatically update this value
     * whenever the entity is modified
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * The line items that make up this purchase order.
     * Each item typically represents a product, quantity, and price.
     * 
     * @OneToMany - One purchase order can have many line items
     * mappedBy="order" - Indicates that the PurchaseOrderItem class
     * has a field named "order" that owns this relationship
     * 
     * Note: No cascade is specified, meaning order and items lifecycles
     * are managed separately (items aren't automatically deleted when an order is).
     */
    @OneToMany(mappedBy = "order")
    private List<PurchaseOrderItem> items;
}
