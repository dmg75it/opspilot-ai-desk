package com.opspilot.desk.integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.openrouter")
@Getter @Setter
public class OpenRouterProperties {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String model = "openai/gpt-3.5-turbo";
    private int maxTokens = 2048;
    private double temperature = 0.7;
    private int timeoutSeconds = 30;
}
