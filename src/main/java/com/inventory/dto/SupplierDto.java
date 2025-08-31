package com.inventory.dto;

import lombok.Data;

@Data
public class SupplierDto {
    private Integer id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    // Do not expose isActive/createdAt/updatedAt unless needed.
}