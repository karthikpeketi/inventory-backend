package com.inventory.service;

import com.inventory.model.User;
import com.sendgrid.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${mail.sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${mail.from.address}")
    private String fromEmail;

    @Value("${mail.from.name:Local Inventory}")
    private String fromName;

    @Override
    public boolean sendEmail(String to, String subject, String htmlContent) {
        // Build SendGrid Mail object
        com.sendgrid.helpers.mail.objects.Email from = new com.sendgrid.helpers.mail.objects.Email(fromEmail, fromName);
        com.sendgrid.helpers.mail.objects.Email toEmail = new com.sendgrid.helpers.mail.objects.Email(to);
        com.sendgrid.helpers.mail.objects.Content content = new com.sendgrid.helpers.mail.objects.Content("text/html", htmlContent);
        com.sendgrid.helpers.mail.Mail mail = new com.sendgrid.helpers.mail.Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            int statusCode = response.getStatusCode();
            // Log for observability
            System.out.println("[Email] To=" + to + ", status=" + statusCode);
            return statusCode >= 200 && statusCode < 300;
        } catch (IOException ex) {
            System.err.println("[Email] Failed sending to " + to + ": " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void sendActivationEmail(User user, String token) {
        String activationLink = frontendUrl + "/activate-account?token=" + token;
        String emailBody = emailTemplateService.getAccountActivationTemplate(user.getFirstName(), activationLink);
        sendEmail(user.getEmail(), "Activate Your Account", emailBody);
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String emailBody = emailTemplateService.getPasswordResetTemplate(resetLink);
        sendEmail(email, "Reset Your Password", emailBody);
    }

    @Override
    public void sendOtpEmail(String email, String firstName, String otpCode, String purpose, int expiryMinutes) {
        String emailBody = emailTemplateService.getOtpTemplate(firstName, otpCode, purpose, expiryMinutes);
        sendEmail(email, "Your Verification Code", emailBody);
    }

    @Override
    public void sendAdminNotificationEmail(String to, String username, String fullName, String email) {
        String emailBody = emailTemplateService.getAdminNotificationTemplate(username, fullName, email);
        sendEmail(to, "New User Registration", emailBody);
    }

    @Override
    public void sendAccountApprovalEmail(String to, String firstName) {
        String loginUrl = frontendUrl + "/login";
        String emailBody = emailTemplateService.getAccountApprovalTemplate(firstName, loginUrl);
        sendEmail(to, "Your Account has been Approved", emailBody);
    }

    @Override
    public void sendAccountRejectionEmail(String to, String reason) {
        String emailBody = emailTemplateService.getAccountRejectionTemplate(reason);
        sendEmail(to, "Your Registration Status", emailBody);
    }

    @Override
    public void sendAccountActivationEmail(String to, String firstName, String token) {
        String activationLink = frontendUrl + "/activate-account?token=" + token;
        String emailBody = emailTemplateService.getAccountActivationTemplate(firstName, activationLink);
        sendEmail(to, "Activate Your Account", emailBody);
    }
}
