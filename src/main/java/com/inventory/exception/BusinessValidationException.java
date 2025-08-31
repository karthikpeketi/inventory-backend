package com.inventory.exception;

/**
 * BusinessValidationException - Custom exception for business rule violations.
 * 
 * This exception is thrown when an operation violates business rules or constraints,
 * such as attempting to delete a supplier with active purchase orders.
 */
public class BusinessValidationException extends RuntimeException {
    
    public BusinessValidationException(String message) {
        super(message);
    }
    
    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}