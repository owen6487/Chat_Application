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
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            Map<String ,Object> dotenvProperties = new HashMap<>();
            addIfAbsent(dotenvProperties , environment ,dotenv ,"GROQ_API_KEY");
            addIfAbsent(dotenvProperties,environment,dotenv,"GROQ_MODEL");
            addIfAbsent(dotenvProperties,environment,dotenv,"GROQ_API_URL");
            addIfAbsent(dotenvProperties,environment,dotenv,"GROQ_MAX_TOKENS");
            addIfAbsent(dotenvProperties,environment,dotenv,"MONGO_URI");

            if(!dotenvProperties.isEmpty()){
                environment.getPropertySources()
                        .addLast(new MapPropertySource("dotenvProperties",dotenvProperties));
            }
        } catch (Exception e) {
            // In production (e.g. Render), .env file won't exist.
            // Spring will use OS environment variables directly via application.yaml.
            System.out.println("Dotenv: .env file not found, using OS environment variables.");
            System.out.println("DEBUG MONGO_URI FROM OS: '" + System.getenv("MONGO_URI") + "'");
        }
    }

    private void addIfAbsent(Map<String, Object> target, ConfigurableEnvironment env, Dotenv dotenv, String key) {
        if (env.containsProperty(key)) return;
        String value = dotenv.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

}



