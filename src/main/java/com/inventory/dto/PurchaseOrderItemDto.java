package com.inventory.dto;
import lombok.Data;

@Data
public class PurchaseOrderItemDto {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Integer receivedQuantity;
    private Double unitPrice;
    private Double total;
    private String notes;
}