package com.inventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * EmailTemplateService provides modern, responsive HTML email templates
 * for various purposes in the Inventory360 system.
 */
@Service
public class EmailTemplateService {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${app.name:Inventory360}")
    private String appName;

    @Value("${app.support.email:support@inventorymanagement360.com}")
    private String supportEmail;

    /**
     * Base HTML template with modern styling
     */
    private String getBaseTemplate(String title, String content) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        line-height: 1.6;
                        color: #333333;
                        background-color: #f8fafc;
                    }
                    
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    
                    .header {
                        background-color: #2563eb;
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                    }
                    
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    
                    .header p {
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    
                    .content {
                        padding: 40px 30px;
                    }
                    
                    .greeting {
                        font-size: 18px;
                        font-weight: 600;
                        color: #1f2937;
                        margin-bottom: 20px;
                    }
                    
                    .message {
                        font-size: 16px;
                        line-height: 1.7;
                        color: #4b5563;
                        margin-bottom: 30px;
                    }
                    
                    .button {
                        display: inline-block;
                        background-color: #2563eb;
                        color: #ffffff;
                        width: fit-content;
                        text-decoration: none;
                        padding: 16px 32px;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 16px;
                        text-align: center;
                        margin: 20px 0;
                        transition: transform 0.2s ease;
                    }
                    
                    .button:hover {
                        transform: translateY(-2px);
                    }
                    
                    .otp-code {
                        background-color: #f3f4f6;
                        border: 2px dashed #d1d5db;
                        border-radius: 8px;
                        padding: 20px;
                        text-align: center;
                        margin: 20px 0;
                    }
                    
                    .otp-code .code {
                        font-size: 32px;
                        font-weight: 700;
                        color: #1f2937;
                        letter-spacing: 4px;
                        font-family: 'Courier New', monospace;
                    }
                    
                    .otp-code .label {
                        font-size: 14px;
                        color: #6b7280;
                        margin-bottom: 10px;
                    }
                    
                    .info-box {
                        background-color: #eff6ff;
                        border-left: 4px solid #3b82f6;
                        padding: 16px;
                        margin: 20px 0;
                        border-radius: 0 8px 8px 0;
                    }
                    
                    .warning-box {
                        background-color: #fef3cd;
                        border-left: 4px solid #f59e0b;
                        padding: 16px;
                        margin: 20px 0;
                        border-radius: 0 8px 8px 0;
                    }
                    
                    .footer {
                        background-color: #f9fafb;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    
                    .footer p {
                        font-size: 14px;
                        color: #6b7280;
                        margin-bottom: 10px;
                    }
                    
                    .footer a {
                        color: #2563eb;
                        text-decoration: none;
                    }
                    
                    .social-links {
                        margin-top: 20px;
                    }
                    
                    .social-links a {
                        display: inline-block;
                        margin: 0 10px;
                        color: #9ca3af;
                        text-decoration: none;
                    }
                    
                    @media only screen and (max-width: 600px) {
                        .email-container {
                            margin: 0;
                            border-radius: 0;
                        }
                        
                        .header, .content, .footer {
                            padding: 20px;
                        }
                        
                        .header h1 {
                            font-size: 24px;
                        }
                    }
                </style>
            </head>
            <body>
                <div style="padding: 20px;">
                    <div class="email-container">
                        <div class="header">
                            <h1 style="text-align: center; margin: 0 0 10px 0;">%s</h1>
                            <p style="text-align: center;">Inventory Management System</p>
                        </div>
                        <div class="content">
                            %s
                        </div>
                        <div class="footer">
                            <p>This email was sent from %s</p>
                            <p>If you have any questions, contact us at <a href="mailto:%s">%s</a></p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, appName, content, appName, supportEmail, supportEmail, appName);
    }

    /**
     * Account activation email template
     */
    public String getAccountActivationTemplate(String firstName, String activationLink) {
        String content = """
            <div class="greeting">Hello %s,</div>
            <div class="message">
                Welcome to %s! Your account has been created and is ready for activation.
                <br><br>
                To get started, please click the button below to activate your account and set your password:
            </div>
            <div style="text-align: center;">
                <a href="%s" class="button" style="color: #ffffff;">Activate Your Account</a>
            </div>
            <div class="info-box">
                <strong>Important:</strong> This activation link will expire in 48 hours for security reasons.
            </div>
            <div class="message">
                If the button doesn't work, you can copy and paste this link into your browser:
                <br><br>
                <a href="%s" style="color: #2563eb; word-break: break-all;">%s</a>
            </div>
            <div class="warning-box">
                If you didn't request this account, please ignore this email and the account will remain inactive.
            </div>
            """.formatted(firstName, appName, activationLink, activationLink, activationLink);

        return getBaseTemplate("Activate Your Account", content);
    }

    /**
     * Password reset email template
     */
    public String getPasswordResetTemplate(String resetLink) {
        String content = """
            <div class="greeting">Password Reset Request</div>
            <div class="message">
                We received a request to reset your password for your %s account.
                <br><br>
                Click the button below to create a new password:
            </div>
            <div style="text-align: center;">
                <a href="%s" class="button" style="color: #ffffff;">Reset Password</a>
            </div>
            <div class="info-box">
                <strong>Security Notice:</strong> This reset link will expire in 1 hour for your security.
            </div>
            <div class="message">
                If the button doesn't work, copy and paste this link into your browser:
                <br><br>
                <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
            </div>
            <div class="warning-box">
                <strong>Important:</strong> If you didn't request this password reset, please ignore this email. 
                Your password will remain unchanged.
            </div>
            """.formatted(appName, resetLink, resetLink, resetLink);

        return getBaseTemplate("Reset Your Password", content);
    }

    /**
     * OTP email template
     */
    public String getOtpTemplate(String firstName, String otpCode, String purpose, int expiryMinutes) {
        String purposeText = switch (purpose.toLowerCase()) {
            case "password_reset" -> "reset your password";
            case "current_email" -> "verify your current email address";
            case "new_email" -> "verify your new email address";
            default -> "complete your verification";
        };

        String content = """
            <div class="greeting">Hello %s,</div>
            <div class="message">
                You requested to %s. Please use the verification code below:
            </div>
            <div class="otp-code">
                <div class="label">Your Verification Code</div>
                <div class="code">%s</div>
            </div>
            <div class="info-box">
                <strong>Security Notice:</strong> This code will expire in %d minutes for your security.
            </div>
            <div class="warning-box">
                <strong>Important:</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                    <li>Never share this code with anyone</li>
                    <li>Our team will never ask for this code</li>
                    <li>If you didn't request this, please ignore this email</li>
                </ul>
            </div>
            """.formatted(firstName, purposeText, otpCode, expiryMinutes);

        return getBaseTemplate("Verification Code", content);
    }

    /**
     * Account approval notification template
     */
    public String getAccountApprovalTemplate(String firstName, String loginUrl) {
        String content = """
            <div class="greeting">Great news, %s!</div>
            <div class="message">
                Your %s account has been approved by our administrator. You can now access the system with full privileges.
            </div>
            <div style="text-align: center;">
                <a href="%s" class="button" style="color: #ffffff;">Login to Your Account</a>
            </div>
            <div class="info-box">
                <strong>What's Next?</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                    <li>Use your existing credentials to log in</li>
                    <li>Explore the inventory management features</li>
                    <li>Contact support if you need any assistance</li>
                </ul>
            </div>
            <div class="message">
                Welcome to the team! We're excited to have you on board.
            </div>
            """.formatted(firstName, appName, loginUrl);

        return getBaseTemplate("Account Approved", content);
    }

    /**
     * Account rejection notification template
     */
    public String getAccountRejectionTemplate(String reason) {
        String content = """
            <div class="greeting">Registration Update</div>
            <div class="message">
                We regret to inform you that your registration request for %s has been declined.
            </div>
            %s
            <div class="info-box">
                <strong>What can you do?</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                    <li>Contact our support team for more information</li>
                    <li>Review our registration requirements</li>
                    <li>Submit a new application if appropriate</li>
                </ul>
            </div>
            <div class="message">
                If you believe this decision was made in error, please don't hesitate to reach out to our support team.
            </div>
            """.formatted(appName, 
                reason != null && !reason.trim().isEmpty() 
                    ? "<div class=\"warning-box\"><strong>Reason:</strong> " + reason + "</div>"
                    : "");

        return getBaseTemplate("Registration Update", content);
    }

    /**
     * Admin notification for new user registration
     */
    public String getAdminNotificationTemplate(String username, String fullName, String email) {
        String content = """
            <div class="greeting">New User Registration</div>
            <div class="message">
                A new user has registered for %s and requires your approval.
            </div>
            <div class="info-box">
                <strong>User Details:</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                    <li><strong>Username:</strong> %s</li>
                    <li><strong>Name:</strong> %s</li>
                    <li><strong>Email:</strong> %s</li>
                </ul>
            </div>
            <div style="text-align: center;">
                <a href="%s/admin/users" class="button" style="color: #ffffff;">Review Registration</a>
            </div>
            <div class="message">
                Please log in to the admin panel to approve or reject this registration request.
            </div>
            """.formatted(appName, username, fullName, email, frontendUrl);

        return getBaseTemplate("New User Registration", content);
    }

    /**
     * Welcome email template for new users
     */
    public String getWelcomeTemplate(String firstName) {
        String content = """
            <div class="greeting">Welcome to %s, %s!</div>
            <div class="message">
                Congratulations! Your account is now active and ready to use. We're thrilled to have you join our inventory management platform.
            </div>
            <div class="info-box">
                <strong>Getting Started:</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                    <li>Explore the dashboard to get familiar with the interface</li>
                    <li>Set up your inventory categories and products</li>
                    <li>Configure your user preferences</li>
                    <li>Check out our help documentation</li>
                </ul>
            </div>
            <div style="text-align: center;">
                <a href="%s/dashboard" class="button" style="color: #ffffff;">Go to Dashboard</a>
            </div>
            <div class="message">
                If you have any questions or need assistance getting started, our support team is here to help!
            </div>
            """.formatted(appName, firstName, frontendUrl);

        return getBaseTemplate("Welcome!", content);
    }
}
