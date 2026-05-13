package com.opspilot.desk.ai;

import com.opspilot.desk.exception.AiProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenRouterChatProvider implements AiChatProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final RestTemplate restTemplate;

    public OpenRouterChatProvider(String apiKey, String baseUrl, String model,
                                   int maxTokens, double temperature, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public AiResponse chat(String systemPrompt, List<AiMessage> messages) {
        long start = System.currentTimeMillis();
        log.info("AI request started model={}", model);

        List<Map<String, String>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));
        for (AiMessage m : messages) {
            allMessages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", allMessages,
                "max_tokens", maxTokens,
                "temperature", temperature
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "https://opspilot.local");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            long elapsed = System.currentTimeMillis() - start;
            String content = extractContent(response);
            Integer tokens = extractTokens(response);
            String usedModel = extractModel(response);

            log.info("AI request completed elapsed={}ms tokens={}", elapsed, tokens);
            return AiResponse.builder()
                    .content(content)
                    .model(usedModel != null ? usedModel : model)
                    .tokenCount(tokens)
                    .build();

        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI request failed elapsed={}ms error={}", elapsed, e.getMessage());
            throw new AiProviderException("OpenRouter request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) throw new AiProviderException("Empty response from OpenRouter");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new AiProviderException("No choices in OpenRouter response");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    private Integer extractTokens(Map<String, Object> response) {
        if (response == null) return null;
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage == null) return null;
        Object total = usage.get("total_tokens");
        return total instanceof Number n ? n.intValue() : null;
    }

    private String extractModel(Map<String, Object> response) {
        if (response == null) return null;
        return (String) response.get("model");
    }
}
