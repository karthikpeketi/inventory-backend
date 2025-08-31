package com.inventory.exception;

/**
 * ResourceNotFoundException – a custom unchecked (runtime) exception.
 *
 * Purpose and Usage:
 * - Used whenever a requested domain resource (user, product, order, etc.) does not exist in the backend.
 * - Meant to be thrown from service or controller layers, and then caught by a global exception handler.
 *   (see GlobalExceptionHandler.java) to return appropriate HTTP error (404) to the REST client.
 * - By subclassing RuntimeException (unchecked) instead of Exception (checked),
 *   you allow the exception to propagate up the stack without explicit try/catch blocks.
 *
 * When to define custom exceptions?
 * - When you want to provide meaningful, type-safe errors ("User not found" distinct from "Internal Error").
 * - For business logic branches, error handling, and clean API error responses.
 *
 * Constructor:
 * - Accepts a string message explaining which resource was not found (useful for client/errors/logs).
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructor that takes a detailed error message
     * @param message Description of which resource was not found and why
     */
    public ResourceNotFoundException(String message) {
        // Passes the message to the parent RuntimeException class
        super(message);
    }
}
