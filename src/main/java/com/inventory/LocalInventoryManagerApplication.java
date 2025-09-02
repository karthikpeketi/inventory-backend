package com.inventory;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class LocalInventoryManagerApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
		System.setProperty("DB_URL", dotenv.get("DB_URL"));
		System.setProperty("DB_USER", dotenv.get("DB_USER"));
		System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
		System.setProperty("MAIL_FROM_NAME", dotenv.get("MAIL_FROM_NAME"));
		System.setProperty("MAIL_FROM_ADDRESS", dotenv.get("MAIL_FROM_ADDRESS"));
		System.setProperty("SENDGRID_API_KEY", dotenv.get("SENDGRID_API_KEY"));
		System.setProperty("CORS_ALLOWED_ORIGINS", dotenv.get("CORS_ALLOWED_ORIGINS"));
		System.setProperty("FRONTEND_URL", dotenv.get("FRONTEND_URL"));
		SpringApplication.run(LocalInventoryManagerApplication.class, args);
	}

}
