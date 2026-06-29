package com.ChatApplication.config;

import io.github.cdimascio.dotenv.Dotenv;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements EnvironmentPostProcessor {
    public DotenvConfig() {
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @Nullable SpringApplication application) {
        
        // 1. Load Dotenv (silently ignores missing file in production)
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Map<String, Object> dotenvProperties = new HashMap<>();
        
        // 2. Add variables to our map
        addIfAbsent(dotenvProperties, environment, dotenv, "GROQ_API_KEY");
        addIfAbsent(dotenvProperties, environment, dotenv, "GROQ_MODEL");
        addIfAbsent(dotenvProperties, environment, dotenv, "GROQ_API_URL");
        addIfAbsent(dotenvProperties, environment, dotenv, "GROQ_MAX_TOKENS");
        addIfAbsent(dotenvProperties, environment, dotenv, "MONGO_URI");

        // 3. Add to Spring environment with HIGH priority (addFirst)
        if (!dotenvProperties.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", dotenvProperties));
        }
        
        // 4. Debug print to verify OS environment variables
        String mongoUri = System.getenv("MONGO_URI");
        if (mongoUri != null && !mongoUri.isEmpty()) {
            System.out.println("DEBUG: Found MONGO_URI from OS Environment Variables.");
            if (mongoUri.contains("localhost")) {
                System.out.println("WARNING: MONGO_URI contains 'localhost'. It will not connect to MongoAtlas!");
            }
        } else {
            System.out.println("DEBUG: MONGO_URI is missing or empty in OS Environment Variables!");
        }
    }

    private void addIfAbsent(Map<String, Object> target, ConfigurableEnvironment env, Dotenv dotenv, String key) {
        if (env.containsProperty(key))
            return;
        String value = dotenv.get(key);
        if (value != null && !value.trim().isEmpty()) {
            target.put(key, value);
        }
    }

}
