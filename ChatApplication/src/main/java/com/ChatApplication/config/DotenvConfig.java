package com.ChatApplication.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Loads environment variables for local development.
 *
 * Spring Boot 4.0+ automatically loads .env files via:
 *   spring.config.import: optional:file:.env[.properties]
 *
 * This class adds a startup validation to fail fast if required
 * environment variables are missing.
 */
@Configuration
public class DotenvConfig {

    /**
     * Validate required environment variables at app startup.
     * Fails immediately if MONGO_URI is not set, preventing
     * confusing MongoDB connection errors later.
     */
    @Bean
    public ApplicationRunner validateEnvironment(Environment env) {
        return args -> {
            String mongoUri = env.getProperty("spring.data.mongodb.uri");

            if (mongoUri == null || mongoUri.isEmpty() || mongoUri.contains("NOT_CONFIGURED")) {
                throw new IllegalStateException(
                        "MONGO_URI environment variable is not set!\n" +
                        "Set it before running the app:\n" +
                        "  - Local: Create .env file with MONGO_URI=mongodb://...\n" +
                        "  - Render: Add MONGO_URI to Environment variables in dashboard\n" +
                        "  - Docker: Pass -e MONGO_URI=mongodb://... to docker run"
                );
            }
        };
    }
}


