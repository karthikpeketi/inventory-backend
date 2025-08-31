package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for account activation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAccountRequest {
    private String token;
    private String password;
}