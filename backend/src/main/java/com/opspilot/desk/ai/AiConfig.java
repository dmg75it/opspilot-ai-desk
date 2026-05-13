package com.opspilot.desk.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class AiConfig {

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.model:openai/gpt-3.5-turbo}")
    private String model;

    @Value("${openrouter.max-tokens:1024}")
    private int maxTokens;

    @Value("${openrouter.temperature:0.7}")
    private double temperature;

    @Value("${openrouter.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    @ConditionalOnProperty(name = "app.fake-ai", havingValue = "false", matchIfMissing = true)
    public AiChatProvider openRouterProvider() {
        log.info("Using OpenRouter AI provider, model={}", model);
        return new OpenRouterChatProvider(apiKey, baseUrl, model, maxTokens, temperature, timeoutSeconds);
    }

    @Bean
    @ConditionalOnProperty(name = "app.fake-ai", havingValue = "true")
    public AiChatProvider fakeAiProvider() {
        log.info("Using Fake AI provider");
        return new FakeAiChatProvider();
    }
}
