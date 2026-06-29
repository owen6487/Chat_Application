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

        addMongoUri(dotenvProperties, environment, dotenv);

        // 3. Add to Spring environment with HIGH priority (addFirst)
        if (!dotenvProperties.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", dotenvProperties));
        }
        
        // 4. Debug print to verify OS environment variables
        if (System.getenv("MONGO_URI") != null) {
            System.out.println("DEBUG: Found MONGO_URI from OS Environment Variables.");
        }
    }

    private void addMongoUri(Map<String, Object> target, ConfigurableEnvironment env, Dotenv dotenv) {
        String springKey = "spring.data.mongodb.uri";
        if (env.containsProperty(springKey))
            return;

        String value = dotenv.get("MONGO_URI");
        if (value != null) {
            target.put(springKey, value);
        }
    }

    private void addMongoUriFromOsEnv(Map<String, Object> target, ConfigurableEnvironment env) {
        String springKey = "spring.data.mongodb.uri";
        if (env.containsProperty(springKey))
            return;

        String value = System.getenv("MONGO_URI");
        if (value != null) {
            target.put(springKey, value);
        }
    }

    private void addIfAbsent(Map<String, Object> target, ConfigurableEnvironment env, Dotenv dotenv, String key) {
        if (env.containsProperty(key))
            return;
        String value = dotenv.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

}
