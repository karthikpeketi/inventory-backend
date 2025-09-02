package com.inventory.service;

import com.inventory.model.User;

public interface EmailService {
    // Generic reusable method
    boolean sendEmail(String to, String subject, String htmlContent);

    // Specific convenience methods used across the app
    void sendActivationEmail(User user, String token);
    void sendPasswordResetEmail(String email, String token);
    void sendOtpEmail(String email, String firstName, String otpCode, String purpose, int expiryMinutes);
    void sendAdminNotificationEmail(String to, String username, String fullName, String email);
    void sendAccountApprovalEmail(String to, String firstName);
    void sendAccountRejectionEmail(String to, String reason);
    void sendAccountActivationEmail(String to, String firstName, String token);
}
