package io.opspilot.desk.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.opspilot.desk.ai.AiClient;
import io.opspilot.desk.ai.FakeAiClient;
import io.opspilot.desk.ai.OpenRouterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfig {

    private final OpenRouterProperties openRouterProps;

    @Bean
    RestClient openRouterRestClient() {
        return RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    AiClient aiClient(RestClient openRouterRestClient) {
        boolean useFake = openRouterProps.isFakeMode()
                || !StringUtils.hasText(openRouterProps.getApiKey());
        if (useFake) {
            log.info("AI client: using FakeAiClient (fake-mode={}, api-key-present={})",
                    openRouterProps.isFakeMode(),
                    StringUtils.hasText(openRouterProps.getApiKey()));
            return new FakeAiClient();
        }
        log.info("AI client: using OpenRouterClient (model={})", openRouterProps.getModel());
        return new OpenRouterClient(openRouterProps, openRouterRestClient);
    }
}
