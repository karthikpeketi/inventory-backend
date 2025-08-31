package com.inventory.controller;

import com.inventory.dto.PurchaseOrderDto;
import com.inventory.service.PurchaseOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.inventory.model.User;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class PurchaseOrderController {
    private final PurchaseOrderService orderService;
    public PurchaseOrderController(PurchaseOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseOrderDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String searchFields) {

        // Log the request parameters for debugging
        System.out.println("Request parameters:");
        System.out.println("page: " + page);
        System.out.println("size: " + size);
        System.out.println("status: " + status);
        System.out.println("search: " + search);
        System.out.println("sortBy: " + sortBy);
        System.out.println("sortDirection: " + sortDirection);
        System.out.println("searchFields: " + searchFields);

        // Create pageable without sorting, as sorting will be handled in the service
        Pageable pageable = PageRequest.of(page, size);

        // Call the service with all parameters
        return ResponseEntity.ok(orderService.getAllOrders(pageable, status, search, sortBy, sortDirection));
    }
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
    @PostMapping
    public ResponseEntity<PurchaseOrderDto> create(@RequestBody PurchaseOrderDto dto) {
        // Retrieve user ID from the request payload
        Integer userId = dto.getCreatedById();
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided in the request");
        }
        // Validate user role (this assumes a method to check user role, which may need to be implemented or injected)
        String userRole = getUserRoleById(userId);
        if (!"ADMIN".equals(userRole) && !"STAFF".equals(userRole)) {
            throw new SecurityException("User does not have permission to create orders");
        }
        return ResponseEntity.ok(orderService.createOrder(dto, userId));
    }
    
    // Helper method to get user role by ID (this is a placeholder; implement actual logic based on your setup)
    private String getUserRoleById(Integer userId) {
        // This should be replaced with actual logic to fetch user role from a UserService or Repository
        // For now, returning a dummy value or fetching from security context as fallback
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();
                    if (role != null && (role.equals("ROLE_ADMIN") || role.equals("ADMIN"))) {
                        return "ADMIN";
                    } else if (role != null && (role.equals("ROLE_STAFF") || role.equals("STAFF"))) {
                        return "STAFF";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving user role: " + e.getMessage());
        }
        // Fallback to a dummy check for demonstration; replace with actual user role lookup
        return userId == 1 ? "ADMIN" : "USER";
    }
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> update(@PathVariable Integer id, @RequestBody PurchaseOrderDto dto) {
        // Retrieve user ID and role from security context
        Integer userId = getCurrentUserId();
        String userRole = getCurrentUserRole();
        
        // Get the current order to check permissions
        PurchaseOrderDto currentOrder = orderService.getOrderById(id);
        if (currentOrder == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user has permission to edit this order
        boolean canEdit = false;
        
        if ("ADMIN".equals(userRole)) {
            // Admin can edit orders that are in Pending or Processing status
            canEdit = "PENDING".equals(currentOrder.getStatus()) || "PROCESSING".equals(currentOrder.getStatus());
        } else if ("STAFF".equals(userRole)) {
            // Staff can only edit their own orders that are in Pending status
            canEdit = "PENDING".equals(currentOrder.getStatus()) && userId.equals(currentOrder.getCreatedById());
        }
        
        if (!canEdit) {
            throw new SecurityException("You don't have permission to edit this order");
        }
        
        return ResponseEntity.ok(orderService.updateOrder(id, dto, userId, userRole));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderDto> updateStatus(
            @PathVariable Integer id, 
            @RequestBody Map<String, String> statusUpdate) {
        // Retrieve user ID and role from security context (assuming Spring Security is used)
        Integer userId = getCurrentUserId();
        String userRole = getCurrentUserRole();
        String status = statusUpdate.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status, userId, userRole));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to get current user ID from security context (kept for other methods if needed)
    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new SecurityException("No authenticated user found in security context.");
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId();
            } else if (principal instanceof com.inventory.security.service.UserDetailsImpl) {
                return ((com.inventory.security.service.UserDetailsImpl) principal).getId();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                throw new IllegalStateException("UserDetails principal found but not of expected type UserDetailsImpl: " + principal.getClass().getName());
            } else {
                throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
            }
        } catch (Exception e) {
            System.err.println("Error retrieving user ID from security context: " + e.getMessage());
            throw new RuntimeException("Unable to retrieve user ID from security context: " + e.getMessage(), e);
        }
    }


    // Helper method to get current user role from security context
    private String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();

                    if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                        return "ADMIN";
                    } else if ("ROLE_STAFF".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
                        return "STAFF";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving user role from security context: " + e.getMessage());
        }

        return "UNKNOWN"; // Safe fallback instead of hardcoded ADMIN
    }

}
