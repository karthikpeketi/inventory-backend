package com.inventory.config;

import org.springframework.context.annotation.Configuration;

/**
 * Deprecated SMTP config kept for reference. Railway free plan blocks SMTP, so we use SendGrid API instead.
 * This class is intentionally empty to avoid creating JavaMailSender beans.
 */
@Configuration
public class MailConfig {
}
