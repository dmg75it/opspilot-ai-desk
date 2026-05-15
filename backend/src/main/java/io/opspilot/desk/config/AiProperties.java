package io.opspilot.desk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String provider = "fake";
    private Openrouter openrouter = new Openrouter();

    @Data
    public static class Openrouter {
        private String apiKey = "";
        private String baseUrl = "https://openrouter.ai/api/v1";
        private String model = "openai/gpt-4o-mini";
        private int timeoutSeconds = 30;
        private int maxTokens = 1024;
        private double temperature = 0.7;
    }
}
