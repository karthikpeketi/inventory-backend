package com.inventory.util;

import com.inventory.dto.*;
import com.inventory.model.*;

public class ModelMapper {
    public static ProductDto mapToProductDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .quantity(product.getQuantity())
                .reorderLevel(product.getReorderLevel())
                .unitPrice(product.getUnitPrice())
                .costPrice(product.getCostPrice())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdAtFormatted(product.getCreatedAt() != null ? product.getCreatedAt().toLocalDate().toString() : null)
                .updatedAtFormatted(product.getUpdatedAt() != null ? product.getUpdatedAt().toLocalDate().toString() : null)
                .build();
    }

    public static CategoryDto mapToCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdAtFormatted(category.getCreatedAt() != null ? category.getCreatedAt().toLocalDate().toString() : null)
                .updatedAtFormatted(category.getUpdatedAt() != null ? category.getUpdatedAt().toLocalDate().toString() : null)
                .build();
    }

    public static InventoryTransactionDto mapToInventoryTransactionDto(InventoryTransaction transaction) {
        return InventoryTransactionDto.builder()
                .id(transaction.getId())
                .productId(transaction.getProduct().getId())
                .productName(transaction.getProduct().getName())
                .userId(transaction.getUser().getId())
                .username(transaction.getUser().getUsername())
                .transactionType(transaction.getTransactionType())
                .quantity(transaction.getQuantity())
                .notes(transaction.getNotes())
                .referenceNumber(transaction.getReferenceNumber())
                .transactionDate(transaction.getTransactionDate())
                .transactionDateFormatted(transaction.getTransactionDate() != null ? transaction.getTransactionDate().toLocalDate().toString() : null)
                .unitPrice(transaction.getProduct() != null ? transaction.getProduct().getUnitPrice() : null)
                .build();
    }

    public static UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .statusReason(user.getStatusReason())

                .lastLoginDate(user.getLastLoginDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdAtFormatted(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : null)
                .updatedAtFormatted(user.getUpdatedAt() != null ? user.getUpdatedAt().toLocalDate().toString() : null)
                .lastLoginDateFormatted(user.getLastLoginDate() != null ? user.getLastLoginDate().toLocalDate().toString() : null)
                .build();
    }
}
