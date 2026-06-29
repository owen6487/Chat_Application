package com.ChatApplication.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        logger.info("Creating MongoClient with URI preview: {}",
                mongoUri != null ? (mongoUri.length() > 40 ? mongoUri.substring(0, 40) + "..." : mongoUri) : "<null>");

        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // fail fast on server selection to surface bad config quickly
                .applyToSocketSettings(builder -> builder.readTimeout((int) Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(10, TimeUnit.SECONDS))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient client) {
        // Use database from connection string, or default to "test"
        String database = new ConnectionString(mongoUri).getDatabase();
        if (database == null || database.isEmpty()) {
            database = "test";
        }
        logger.info("MongoTemplate will use database: {}", database);
        return new MongoTemplate(client, database);
    }
}


