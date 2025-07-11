package com.shopper.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JavaBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(JavaBackendApplication.class, args);
        System.out.println("🚀 Java Backend Server is running!");
        System.out.println("📱 Environment: " + System.getProperty("spring.profiles.active", "development"));
    }
}