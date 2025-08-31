package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    // Constants for user status reasons
    public static final String STATUS_PENDING_ACTIVATION = "PENDING_ACTIVATION"; // Admin created user
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL"; // Self-registered user
    public static final String STATUS_DEACTIVATED = "DEACTIVATED_BY_ADMIN"; // Admin deactivated user
    public static final String STATUS_REJECTED = "REJECTED_BY_ADMIN"; // Admin rejected user
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    private String firstName;
    private String lastName;
    
    @Column(nullable = false)
    private String role; // ADMIN, STAFF
    
    @Column(nullable = false)
    private Boolean isActive = true;
    

    
    @Column(name = "status_reason")
    private String statusReason; // Reason for current status (e.g., "PENDING_ACTIVATION", "PENDING_APPROVAL", "DEACTIVATED_BY_ADMIN")
    
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user")
    private List<InventoryTransaction> transactions;
    
    @OneToMany(mappedBy = "createdBy")
    private List<PurchaseOrder> purchaseOrders;


}