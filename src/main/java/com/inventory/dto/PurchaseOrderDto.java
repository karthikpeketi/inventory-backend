package com.inventory.dto;


import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderDto {
    private Integer id;
    private String orderNumber;
    private String supplierName;
    private Integer supplierId;
    private String status;
    private double totalAmount;
private String orderDate;
    private String expectedDeliveryDate;
    private int itemCount;
    private String notes;
    private String createdByName;
    private Integer createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemDto> items;
}
