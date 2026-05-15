package io.opspilot.desk.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opspilot.desk.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter")
@Slf4j
public class OpenRouterClient implements AiClient {
    private final RestClient restClient;
    private final AiProperties props;

    public OpenRouterClient(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
            .baseUrl(props.getOpenrouter().getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + props.getOpenrouter().getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        var messages = buildMessages(request);
        var body = Map.of(
            "model", props.getOpenrouter().getModel(),
            "messages", messages,
            "max_tokens", props.getOpenrouter().getMaxTokens(),
            "temperature", props.getOpenrouter().getTemperature()
        );

        log.info("AI request model={} messages={}", props.getOpenrouter().getModel(), messages.size());
        long start = System.currentTimeMillis();
        try {
            var response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(OpenRouterResponse.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI response elapsed={}ms model={}", elapsed, props.getOpenrouter().getModel());

            var choice = response.choices().get(0);
            var usage = response.usage();
            return new AiResponse(
                choice.message().content(),
                usage != null ? usage.promptTokens() : 0,
                usage != null ? usage.completionTokens() : 0,
                props.getOpenrouter().getModel(),
                null
            );
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI request failed elapsed={}ms error={}", elapsed, e.getMessage());
            return new AiResponse(null, 0, 0, props.getOpenrouter().getModel(), e.getMessage());
        }
    }

    private List<Map<String, String>> buildMessages(AiRequest request) {
        var msgs = new ArrayList<Map<String, String>>();
        if (request.systemPrompt() != null) {
            msgs.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        request.messages().forEach(m -> msgs.add(Map.of("role", m.role(), "content", m.content())));
        return msgs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterResponse(List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                 @JsonProperty("completion_tokens") int completionTokens) {}
}
