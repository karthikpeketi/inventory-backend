package com.inventory.service;

import com.inventory.dto.PurchaseOrderDto;
import com.inventory.dto.PurchaseOrderItemDto;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderItem;
import com.inventory.model.Product;
import com.inventory.model.Supplier;
import com.inventory.model.User;
import com.inventory.model.InventoryTransaction;
import com.inventory.repository.PurchaseOrderItemRepository;
import com.inventory.repository.PurchaseOrderRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;

    public PurchaseOrderServiceImpl(
            PurchaseOrderRepository purchaseOrderRepository, 
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            ProductRepository productRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Page<PurchaseOrderDto> getAllOrders(Pageable pageable, String status, String search, String sortBy, String sortDirection) {
        // Normalize status to match frontend values (lowercase)
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            status = status.toUpperCase(); // Ensure status is uppercase to match entity values
        } else {
            status = null; // Set to null if "all" or empty to fetch all statuses
        }

        // Handle special case for sorting by createdByName (users.username)
        Page<PurchaseOrder> orders;
        
        if (sortBy.equals("users.username")) {
            // For sorting by user's username, use a special query
            // Create a pageable without sort since we'll handle sorting in the query
            Pageable pageableWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            
            // Use the special repository method for sorting by username based on direction
            if (sortDirection.equalsIgnoreCase("desc")) {
                orders = purchaseOrderRepository.findOrdersWithFiltersSortByUsernameDesc(
                    status, search, pageableWithoutSort);
                System.out.println("Using special query for sorting by username DESC");
            } else {
                orders = purchaseOrderRepository.findOrdersWithFiltersSortByUsernameAsc(
                    status, search, pageableWithoutSort);
                System.out.println("Using special query for sorting by username ASC");
            }
        } else {
            // For all other fields, use the standard approach
            Sort sort = Sort.by(sortBy);
            
            // Apply sort direction
            if (sortDirection.equalsIgnoreCase("desc")) {
                sort = sort.descending();
            }
            
            // Create Pageable object with sorting
            Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            
            // Get filtered and sorted data using the standard query
            orders = purchaseOrderRepository.findOrdersWithFilters(
                status, search, pageableWithSort);
                
            System.out.println("Sorting by: " + sortBy + ", Direction: " + sortDirection);
            System.out.println("Applied sort: " + sort);
        }

        // Map to DTOs
        return orders.map(this::toDto);
    }

    @Override
    public PurchaseOrderDto getOrderById(Integer id) {
        Optional<PurchaseOrder> order = purchaseOrderRepository.findById(id);
        return order.map(this::toDto).orElse(null);
    }

    @Override
    public PurchaseOrderDto createOrder(PurchaseOrderDto dto, Integer userId) {
        PurchaseOrder order = toEntity(dto);
        // Set user ID
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            order.setCreatedBy(user);
        } else {
            User user = new User();
            user.setId(1); // Fallback to 1 if userId is null
            order.setCreatedBy(user);
        }
        // Generate order number
        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        // Save order items explicitly if cascading does not work
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (PurchaseOrderItem item : order.getItems()) {
                item.setOrder(saved); // Ensure the order reference is set
                purchaseOrderItemRepository.save(item);
            }
        }
        return toDto(saved);
    }
    
    private String generateOrderNumber() {
        // Fetch the last order number from the database
        List<PurchaseOrder> lastOrders = purchaseOrderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        long nextId = 10001; // Starting point if no orders exist
        if (!lastOrders.isEmpty()) {
            PurchaseOrder lastOrder = lastOrders.get(0);
            String lastOrderNumber = lastOrder.getOrderNumber();
            if (lastOrderNumber != null && lastOrderNumber.startsWith("PO-")) {
                try {
                    long lastNumber = Long.parseLong(lastOrderNumber.substring(3));
                    nextId = lastNumber + 1;
                } catch (NumberFormatException e) {
                    // If parsing fails, fallback to using the ID or default
                    nextId = lastOrder.getId() != null ? lastOrder.getId() + 10001 : 10001;
                }
            } else {
                nextId = lastOrder.getId() != null ? lastOrder.getId() + 10001 : 10001;
            }
        }
        return "PO-" + nextId;
    }

    @Override
    @Transactional
    public PurchaseOrderDto updateOrder(Integer id, PurchaseOrderDto dto, Integer userId, String userRole) {
        // Get the existing order
        PurchaseOrder existingOrder = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        // Validate permissions based on role and order status
        boolean canEdit = false;
        
        if ("ADMIN".equals(userRole)) {
            // Admin can edit orders that are in Pending or Processing status
            canEdit = "PENDING".equals(existingOrder.getStatus()) || "PROCESSING".equals(existingOrder.getStatus());
        } else if ("STAFF".equals(userRole)) {
            // Staff can only edit their own orders that are in Pending status
            canEdit = "PENDING".equals(existingOrder.getStatus()) && 
                      existingOrder.getCreatedBy() != null && 
                      existingOrder.getCreatedBy().getId().equals(userId);
        }
        
        if (!canEdit) {
            throw new SecurityException("You don't have permission to edit this order");
        }
        
        // Convert DTO to entity
        PurchaseOrder updatedOrder = toEntity(dto);
        updatedOrder.setId(id);
        
        // Preserve the original created by user
        updatedOrder.setCreatedBy(existingOrder.getCreatedBy());
        
        // Preserve the original order number
        updatedOrder.setOrderNumber(existingOrder.getOrderNumber());
        
        // Preserve the original creation timestamp
        updatedOrder.setCreatedAt(existingOrder.getCreatedAt());
        
        // Save the updated order
        PurchaseOrder saved = purchaseOrderRepository.save(updatedOrder);
        
        // Update order items using differential update approach
        if (updatedOrder.getItems() != null && !updatedOrder.getItems().isEmpty()) {
            // Get existing items
            List<PurchaseOrderItem> existingItems = purchaseOrderItemRepository.findByOrderId(id);
            Map<Integer, PurchaseOrderItem> existingItemMap = new HashMap<>();
            
            // Create a map of existing items by product ID for easy lookup
            for (PurchaseOrderItem item : existingItems) {
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    existingItemMap.put(item.getProduct().getId(), item);
                }
            }
            
            // Track which items we've processed
            Set<Integer> processedItems = new HashSet<>();
            
            // Update existing items or add new ones
            for (PurchaseOrderItem newItem : updatedOrder.getItems()) {
                // Validate product and product ID
                if (newItem.getProduct() == null || newItem.getProduct().getId() == null) {
                    throw new IllegalArgumentException("Product information is missing for an order item");
                }
                
                Integer productId = newItem.getProduct().getId();
                newItem.setOrder(saved);
                
                // Ensure quantity is not null
                if (newItem.getQuantity() == null) {
                    newItem.setQuantity(0); // Default to 0 or a sensible default
                }
                
                // Ensure unit price is not null
                if (newItem.getUnitPrice() == null) {
                    newItem.setUnitPrice(0.0); // Default to 0 or a sensible default
                }
                
                if (existingItemMap.containsKey(productId)) {
                    // Update existing item
                    PurchaseOrderItem existingItem = existingItemMap.get(productId);
                    newItem.setId(existingItem.getId()); // Preserve the ID
                    
                    // Preserve any fields that might not be in the DTO
                    if (newItem.getReceivedQuantity() == null) {
                        newItem.setReceivedQuantity(existingItem.getReceivedQuantity() != null ? 
                                                   existingItem.getReceivedQuantity() : 0);
                    }
                    
                    purchaseOrderItemRepository.save(newItem);
                    processedItems.add(productId);
                } else {
                    // Add new item
                    // Ensure received quantity is not null for new items
                    if (newItem.getReceivedQuantity() == null) {
                        newItem.setReceivedQuantity(0);
                    }
                    
                    purchaseOrderItemRepository.save(newItem);
                }
            }
            
            // Delete items that are no longer in the updated list
            for (PurchaseOrderItem existingItem : existingItems) {
                if (existingItem.getProduct() != null && existingItem.getProduct().getId() != null) {
                    Integer productId = existingItem.getProduct().getId();
                    if (!processedItems.contains(productId)) {
                        purchaseOrderItemRepository.deleteById(existingItem.getId());
                    }
                }
            }
        } else {
            // If the updated order has no items, delete all existing items
            purchaseOrderItemRepository.deleteByOrderId(id);
        }
        
        return toDto(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderDto updateOrderStatus(Integer id, String status, Integer userId, String userRole) {
        // Validate status
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        
        // Normalize status to uppercase to match entity values
        status = status.toUpperCase();
        
        // Validate that status is one of the allowed values
        if (!status.equals("PENDING") && !status.equals("PROCESSING") && 
            !status.equals("DELIVERED") && !status.equals("CANCELLED")) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        
        // Get the order
        PurchaseOrder order = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        // Check for valid status transitions
        String currentStatus = order.getStatus();
        
        // If trying to set status to DELIVERED, use the completeOrder method instead
        if (status.equals("DELIVERED")) {
            return completeOrder(id, userId, userRole);
        }
        
        // Validate status transitions
        if (currentStatus.equals("DELIVERED") && !status.equals("DELIVERED")) {
            throw new IllegalStateException("Cannot change status from DELIVERED to " + status);
        }
        
        if (currentStatus.equals("CANCELLED") && !status.equals("CANCELLED")) {
            throw new IllegalStateException("Cannot change status from CANCELLED to " + status);
        }
        
        // Update the status
        order.setStatus(status);
        
        // If cancelling an order, add a note
        if (status.equals("CANCELLED")) {
            String cancelNote = "Order cancelled on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String existingNotes = order.getNotes();
            order.setNotes(existingNotes != null ? existingNotes + "\n" + cancelNote : cancelNote);
        }
        
        // Save the updated order
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        
        // Return the updated order as DTO
        return toDto(updatedOrder);
    }

    @Override
    public void deleteOrder(Integer id) {
        purchaseOrderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PurchaseOrderDto completeOrder(Integer id, Integer userId, String userRole) {
        // Get the order
        PurchaseOrder order = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        // Validate that the order is in PROCESSING status
        if (!"PROCESSING".equals(order.getStatus())) {
            throw new IllegalStateException("Only orders in PROCESSING status can be completed. Current status: " + order.getStatus());
        }
        
        // Get the user who is completing the order
        User user = new User();
        user.setId(userId != null ? userId : 1); // Fallback to user ID 1 if not provided
        
        // Update the order status to DELIVERED
        order.setStatus("DELIVERED");
        
        // Process each order item to update inventory
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (PurchaseOrderItem item : order.getItems()) {
                // Get the product
                Product initialProduct = item.getProduct();
                Integer productId = initialProduct.getId();
                
                // Ensure we have the latest product data
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
                
                // Get the received quantity (use the item quantity if receivedQuantity is not set)
                Integer receivedQuantity = item.getReceivedQuantity();
                if (receivedQuantity == null || receivedQuantity == 0) {
                    receivedQuantity = item.getQuantity();
                    item.setReceivedQuantity(receivedQuantity); // Update the received quantity
                    purchaseOrderItemRepository.save(item); // Save the updated item
                }
                
                // Update the product quantity
                Integer currentQuantity = product.getQuantity();
                product.setQuantity(currentQuantity + receivedQuantity);
                productRepository.save(product); // Save the updated product
                
                // Create an inventory transaction for this item
                InventoryTransaction transaction = new InventoryTransaction();
                transaction.setProduct(product);
                transaction.setUser(user);
                transaction.setTransactionType("STOCK_IN");
                transaction.setQuantity(receivedQuantity);
                transaction.setReferenceNumber("PO:" + order.getOrderNumber());
                transaction.setNotes("Stock received from purchase order: " + order.getOrderNumber());
                transaction.setTransactionDate(LocalDateTime.now());
                
                // Save the transaction
                inventoryTransactionRepository.save(transaction);
            }
        }
        
        // Save the updated order
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        
        // Return the updated order as DTO
        return toDto(updatedOrder);
    }

    // ==== Dashboard methods ====

    @Override
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    @Override
    public List<PurchaseOrder> getRecentOrders(int count) {
        return purchaseOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(PurchaseOrder::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public double getRevenueForCurrentMonth() {
        YearMonth now = YearMonth.now();
        return purchaseOrderRepository.findAll().stream()
                .filter(order -> {
                    LocalDateTime dateTime = order.getOrderDate();
                    if (dateTime == null) return false;
                    return dateTime.toLocalDate().getYear() == now.getYear() &&
                        dateTime.toLocalDate().getMonthValue() == now.getMonthValue();
                })
                .mapToDouble(order -> {
                    Double amt = order.getTotalAmount();
                    return amt != null ? amt : 0.0;
                })
                .sum();
    }

    @Override
    public List<Map<String, Object>> getMonthlySalesData(int months) {
        try {
            List<PurchaseOrder> allOrders = purchaseOrderRepository.findAll();
            YearMonth now = YearMonth.now();
            Map<YearMonth, Double> salesByMonth = new LinkedHashMap<>();
            
            // Initialize all months with zero
            for (int i = months - 1; i >= 0; i--) {
                YearMonth y = now.minusMonths(i);
                salesByMonth.put(y, 0.0);
            }
            
            // Sum up sales by month
            for (PurchaseOrder order : allOrders) {
                try {
                    LocalDateTime dateTime = order.getOrderDate();
                    if (dateTime == null) continue;
                    
                    LocalDate localDate = dateTime.toLocalDate();
                    YearMonth ym = YearMonth.from(localDate);
                    
                    if (salesByMonth.containsKey(ym)) {
                        // Handle primitive double value safely
                        double amt = order.getTotalAmount();
                        double sum = salesByMonth.get(ym);
                        salesByMonth.put(ym, sum + amt);
                    }
                } catch (Exception e) {
                    // Log and skip any problematic orders
                    System.err.println("Error processing order: " + e.getMessage());
                }
            }
            
            // Convert to result format
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<YearMonth, Double> entry : salesByMonth.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                String mon = entry.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                row.put("month", mon);
                row.put("sales", entry.getValue());
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error generating monthly sales data: " + e.getMessage());
            e.printStackTrace();
            // Return empty data rather than failing
            return new ArrayList<>();
        }
    }

    // --- Mapping Methods ---

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setOrderNumber(po.getOrderNumber() != null ? po.getOrderNumber() : "");
        
        // Set supplier information
        if (po.getSupplier() != null) {
            dto.setSupplierName(po.getSupplier().getName() != null ? po.getSupplier().getName() : "");
            dto.setSupplierId(po.getSupplier().getId());
        }
        
        dto.setStatus(po.getStatus());
        dto.setTotalAmount(po.getTotalAmount());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        dto.setOrderDate(po.getOrderDate() != null ? po.getOrderDate().format(formatter) : null);
        dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().format(formatter) : null);
        dto.setItemCount(po.getItems() == null ? 0 : po.getItems().size());
        dto.setCreatedByName(po.getCreatedBy() != null ? po.getCreatedBy().getUsername() : null);
        dto.setCreatedById(po.getCreatedBy() != null ? po.getCreatedBy().getId() : null);
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        dto.setNotes(po.getNotes());
        if (po.getItems() != null)
            dto.setItems(po.getItems().stream().map(this::toItemDto).collect(Collectors.toList()));
        return dto;
    }

    private PurchaseOrderItemDto toItemDto(PurchaseOrderItem item) {
        PurchaseOrderItemDto dto = new PurchaseOrderItemDto();
        dto.setId(item.getId());
        
        // Set product information
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductSku(item.getProduct().getSku());
        }
        
        // Handle null values with defaults
        dto.setQuantity(item.getQuantity() != null ? item.getQuantity() : 0);
        dto.setReceivedQuantity(item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0);
        dto.setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : 0.0);
        
        // Calculate total with null checks
        Double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        dto.setTotal(unitPrice * quantity);
        
        dto.setNotes(item.getNotes());
        return dto;
    }

    private PurchaseOrder toEntity(PurchaseOrderDto dto) {
        PurchaseOrder po = new PurchaseOrder();
        // Set supplier
        if (dto.getSupplierId() != null) {
            Supplier supplier = new Supplier();
            supplier.setId(dto.getSupplierId());
            po.setSupplier(supplier);
        }
        // Set other fields
        po.setTotalAmount(dto.getTotalAmount());
        po.setNotes(dto.getNotes());
        po.setStatus(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "PENDING");
        // Parse and set dates
        try {
            if (dto.getOrderDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime orderDate = LocalDate.parse(dto.getOrderDate(), formatter).atStartOfDay();
                po.setOrderDate(orderDate);
            }
        } catch (Exception e) {
            // If parsing fails, try alternative format or set to current date
            try {
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDateTime orderDate = LocalDate.parse(dto.getOrderDate(), altFormatter).atStartOfDay();
                po.setOrderDate(orderDate);
            } catch (Exception e2) {
                po.setOrderDate(LocalDateTime.now());
            }
        }
        try {
            if (dto.getExpectedDeliveryDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime expectedDeliveryDate = LocalDate.parse(dto.getExpectedDeliveryDate(), formatter).atStartOfDay();
                po.setExpectedDeliveryDate(expectedDeliveryDate);
            }
        } catch (Exception e) {
            // If parsing fails, try alternative format or leave as null
            try {
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDateTime expectedDeliveryDate = LocalDate.parse(dto.getExpectedDeliveryDate(), altFormatter).atStartOfDay();
                po.setExpectedDeliveryDate(expectedDeliveryDate);
            } catch (Exception e2) {
                po.setExpectedDeliveryDate(null);
            }
        }
        // Map order items
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<PurchaseOrderItem> items = new ArrayList<>();
            for (PurchaseOrderItemDto itemDto : dto.getItems()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                if (itemDto.getProductId() != null) {
                    Product product = new Product();
                    product.setId(itemDto.getProductId());
                    item.setProduct(product);
                } else {
                    throw new IllegalArgumentException("Product ID is required for order items");
                }
                
                // Ensure quantity is not null
                if (itemDto.getQuantity() != null) {
                    item.setQuantity(itemDto.getQuantity());
                } else {
                    item.setQuantity(0); // Default to 0 or another sensible default
                }
                
                // Ensure unit price is not null
                if (itemDto.getUnitPrice() != null) {
                    item.setUnitPrice(itemDto.getUnitPrice());
                } else {
                    item.setUnitPrice(0.0); // Default to 0 or another sensible default
                }
                
                // Set notes if available
                item.setNotes(itemDto.getNotes());
                
                // Set received quantity if available, otherwise default to 0
                Integer receivedQty = itemDto.getReceivedQuantity();
                item.setReceivedQuantity(receivedQty != null ? receivedQty : 0);
                
                item.setOrder(po); // Set the reference to the order
                items.add(item);
            }
            po.setItems(items);
        }
        return po;
    }
}
