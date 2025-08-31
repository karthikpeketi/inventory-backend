package com.inventory.service;

import com.inventory.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
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
