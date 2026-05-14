package com.opspilot.desk.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter")
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAiProvider implements AiProvider {

    private final OpenRouterProperties properties;

    @Override
    public AiResponse chat(String systemPrompt, List<AiMessage> messages) {
        long start = System.currentTimeMillis();
        log.info("OpenRouter request: model={}, messages={}", properties.getModel(), messages.size());
        try {
            RestTemplate restTemplate = new RestTemplate();

            List<Map<String, String>> chatMessages = new ArrayList<>();
            chatMessages.add(Map.of("role", "system", "content", systemPrompt));
            for (AiMessage m : messages) {
                chatMessages.add(Map.of("role", m.role(), "content", m.content()));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("messages", chatMessages);
            requestBody.put("max_tokens", properties.getMaxTokens());
            requestBody.put("temperature", properties.getTemperature());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getApiKey());
            headers.set("HTTP-Referer", "https://opspilot.ai");
            headers.set("X-Title", "OpsPilot AI Desk");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenRouterResponse> response = restTemplate.exchange(
                properties.getBaseUrl() + "/chat/completions",
                HttpMethod.POST,
                entity,
                OpenRouterResponse.class
            );

            long elapsed = System.currentTimeMillis() - start;
            OpenRouterResponse body = response.getBody();
            if (body != null && body.choices() != null && !body.choices().isEmpty()) {
                String content = body.choices().get(0).message().content();
                int tokens = body.usage() != null ? body.usage().totalTokens() : content.length() / 4;
                log.info("OpenRouter response received: model={}, tokens={}, elapsed={}ms",
                    properties.getModel(), tokens, elapsed);
                return AiResponse.success(content, properties.getModel(), tokens);
            }
            return AiResponse.failure("Empty response from OpenRouter", properties.getModel());
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("OpenRouter call failed after {}ms: {}", elapsed, e.getMessage());
            return AiResponse.failure("AI service temporarily unavailable: " + e.getMessage(), properties.getModel());
        }
    }

    @Override
    public String getModelName() {
        return properties.getModel();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterResponse(List<Choice> choices, Usage usage) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("total_tokens") int totalTokens) {}
}
