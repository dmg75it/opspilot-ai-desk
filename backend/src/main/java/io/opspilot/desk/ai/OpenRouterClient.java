package io.opspilot.desk.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opspilot.desk.config.OpenRouterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
public class OpenRouterClient implements AiClient {

    private final OpenRouterProperties props;
    private final RestClient restClient;

    public OpenRouterClient(OpenRouterProperties props, RestClient restClient) {
        this.props = props;
        this.restClient = restClient;
    }

    @Override
    public AiResponse chat(List<AiMessage> messages) {
        long start = System.currentTimeMillis();
        log.info("AI request: model={} messages={}", props.getModel(), messages.size());

        var requestBody = Map.of(
                "model", props.getModel(),
                "messages", messages,
                "max_tokens", props.getMaxTokens(),
                "temperature", props.getTemperature()
        );

        try {
            var response = restClient.post()
                    .uri(props.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("HTTP-Referer", "https://github.com/opspilot-ai-desk")
                    .body(requestBody)
                    .retrieve()
                    .body(OpenRouterResponse.class);

            long elapsed = System.currentTimeMillis() - start;
            String model = response != null && response.model() != null ? response.model() : props.getModel();
            Integer tokens = response != null && response.usage() != null ? response.usage().completionTokens() : null;
            String content = extractContent(response);

            log.info("AI response: model={} tokens={} elapsed={}ms", model, tokens, elapsed);
            return new AiResponse(content, model, tokens);
        } catch (Exception e) {
            log.error("AI request failed after {}ms: {}", System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    private String extractContent(OpenRouterResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "No response from AI provider.";
        }
        var message = response.choices().get(0).message();
        return message != null ? message.content() : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterResponse(String model, List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}
}
