package io.opspilot.desk.ai;

import io.opspilot.desk.config.OpenRouterProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test against the real OpenRouter API.
 * Skipped automatically when OPENROUTER_API_KEY is not set in the environment.
 */
class OpenRouterClientIT {

    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Skipped: OPENROUTER_API_KEY not set");

        OpenRouterProperties props = new OpenRouterProperties();
        props.setApiKey(apiKey);
        props.setBaseUrl(System.getenv().getOrDefault("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"));
        props.setModel(System.getenv().getOrDefault("OPENROUTER_MODEL", "openai/gpt-3.5-turbo"));
        props.setTimeoutSeconds(30);
        props.setMaxTokens(200);
        props.setTemperature(0.3);

        client = new OpenRouterClient(props, RestClient.create());
    }

    @Test
    void chat_simpleQuestion_returnsNonEmptyResponse() {
        List<AiMessage> messages = List.of(
                new AiMessage("system", "You are a helpful assistant."),
                new AiMessage("user", "Reply with exactly: OPSPILOT_OK")
        );

        AiResponse response = client.chat(messages);

        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.model()).isNotBlank();
        System.out.println("Model: " + response.model());
        System.out.println("Tokens: " + response.tokensUsed());
        System.out.println("Content: " + response.content());
    }

    @Test
    void chat_ticketSummaryPrompt_returnsUsefulResponse() {
        String ticketContext = """
                Ticket context:
                - Title: Package not delivered after 3 days
                - Status: IN_PROGRESS
                - Priority: HIGH
                - Category: DELIVERY
                - Description: Customer ordered on Monday, tracking shows 'in transit' since Tuesday.
                  No updates since then. Customer called twice asking for info.
                Please provide a concise summary and the recommended next action.
                """;

        List<AiMessage> messages = List.of(
                PromptTemplates.systemMessage(),
                new AiMessage("user", ticketContext)
        );

        AiResponse response = client.chat(messages);

        assertThat(response.content()).isNotBlank();
        System.out.println("Summary response:\n" + response.content());
        System.out.println("Model: " + response.model() + " | Tokens: " + response.tokensUsed());
    }

    @Test
    void chat_suggestedReplyPrompt_returnsProfessionalText() {
        String ticketContext = PromptTemplates.buildTicketContext(
                buildSampleTicket()
        ) + "\nPlease draft a professional customer-facing reply for this ticket.";

        List<AiMessage> messages = List.of(
                PromptTemplates.systemMessage(),
                new AiMessage("user", ticketContext)
        );

        AiResponse response = client.chat(messages);

        assertThat(response.content()).isNotBlank();
        System.out.println("Suggested reply:\n" + response.content());
    }

    @Test
    void chat_multiTurn_maintainsContext() {
        List<AiMessage> messages = List.of(
                PromptTemplates.systemMessage(),
                new AiMessage("user", "A ticket about a missing delivery has status HIGH priority."),
                new AiMessage("assistant", "I understand. This is a high-priority delivery issue."),
                new AiMessage("user", "What category should this ticket be classified as?")
        );

        AiResponse response = client.chat(messages);

        assertThat(response.content()).isNotBlank();
        System.out.println("Multi-turn response: " + response.content());
    }

    // Minimal stub ticket for prompt building without DB
    private io.opspilot.desk.entity.Ticket buildSampleTicket() {
        var user = io.opspilot.desk.entity.User.builder()
                .id(java.util.UUID.randomUUID())
                .email("operator@example.com")
                .fullName("Operator User")
                .role(io.opspilot.desk.entity.Role.OPERATOR)
                .build();
        return io.opspilot.desk.entity.Ticket.builder()
                .id(java.util.UUID.randomUUID())
                .title("Parcel lost in transit")
                .description("Customer reports parcel has not arrived 5 days after expected delivery. Last tracking event: sorting center.")
                .status(io.opspilot.desk.entity.TicketStatus.IN_PROGRESS)
                .priority(io.opspilot.desk.entity.TicketPriority.HIGH)
                .category(io.opspilot.desk.entity.TicketCategory.DELIVERY)
                .createdBy(user)
                .build();
    }
}
