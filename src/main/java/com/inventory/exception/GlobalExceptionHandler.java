package com.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 * 
 * Purpose:
 * - Provides a centralized place for handling exceptions thrown by controllers
 *   across the entire Spring Boot application.
 * - Ensures that errors are translated into consistent, structured HTTP JSON responses.
 * - Improves maintainability by preventing clutter of try-catch in every controller method.
 * 
 * Key concepts:
 * - @ControllerAdvice makes this logic "global," impacting all @Controller classes.
 * - Each handler method is decorated with @ExceptionHandler and will only trigger for its specified exception type(s).
 * - You can return custom JSON, control HTTP status codes, and log issues in a uniform way.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles cases where a requested resource (e.g., product, user) was not found.
     * 
     * Mechanism:
     * - Whenever a ResourceNotFoundException is thrown from any controller/service,
     *   this handler processes it.
     * - Returns a response with HTTP 404 (NOT FOUND) and a JSON body with a timestamp and the error message.
     * 
     * @param ex      The thrown exception object (can extract error details)
     * @param request The current web request (rarely used except for logging/context)
     * @return ResponseEntity with structured error body and proper status code
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        // Construct a structured response body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now()); // Current time for logging/debug
        body.put("message", ex.getMessage());       // Message included in exception (e.g., "User not found")
        
        // Send 404 status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handles cases where a resource already exists (e.g., duplicate category name).
     * 
     * @param ex      The thrown exception object
     * @param request The current web request
     * @return ResponseEntity with structured error body and 409 CONFLICT status code
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Object> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        
        // Send 409 CONFLICT status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
    
    /**
     * Handles business validation exceptions.
     * 
     * This handler processes exceptions thrown when business rules are violated,
     * such as attempting to delete a supplier with active purchase orders.
     * 
     * @param ex      The thrown business validation exception
     * @param request The current web request
     * @return ResponseEntity with structured error body and 400 BAD REQUEST status code
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<Object> handleBusinessValidationException(
            BusinessValidationException ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        
        // Send 400 BAD REQUEST status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions when an account is inactive.
     * 
     * This handler processes exceptions thrown when a user attempts to access the system
     * with an inactive account, returning a 403 Forbidden status code.
     * 
     * @param ex      The thrown account inactive exception
     * @param request The current web request
     * @return ResponseEntity with structured error body and 403 FORBIDDEN status code
     */
    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<Object> handleAccountInactiveException(
            AccountInactiveException ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        
        // Send 403 FORBIDDEN status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handles illegal state exceptions.
     * 
     * This handler processes exceptions thrown when an operation cannot be performed
     * due to the current state of the object, such as attempting to delete a category
     * that has associated products.
     * 
     * @param ex      The thrown illegal state exception
     * @param request The current web request
     * @return ResponseEntity with structured error body and 400 BAD REQUEST status code
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        
        // Send 400 BAD REQUEST status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions for duplicate field errors.
     *
     * @param ex The thrown DuplicateFieldException
     * @param request The current web request
     * @return ResponseEntity with structured error body and 409 CONFLICT status code
     */
    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Object> handleDuplicateFieldException(
            DuplicateFieldException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage()); // Use the exception message directly

        // Send 409 CONFLICT status with error info in the body
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles all other (uncaught or unclassified) exceptions globally.
     * - Prevents stack traces or Java-isms from being exposed to the client.
     * - Returns a generic error message with HTTP 500 (INTERNAL SERVER ERROR).
     * 
     * This "catch-all" ensures the client always receives a consistent structure
     * and application's internal details stay private.
     * 
     * @param ex      The thrown exception
     * @param request The web request context
     * @return HTTP 500 response with timestamp, generic error message, and details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(
            Exception ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        // A generic (non-sensitive) message for production safety
        body.put("message", "An error occurred");
        // The actual cause for debugging; careful with sensitive info in production!
        body.put("details", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
