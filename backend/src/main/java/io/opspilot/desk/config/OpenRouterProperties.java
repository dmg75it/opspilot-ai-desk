package io.opspilot.desk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.openrouter")
public class OpenRouterProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private int timeoutSeconds;
    private int maxTokens;
    private double temperature;
    private boolean fakeMode;
}
