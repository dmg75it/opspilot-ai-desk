package io.opspilot.desk.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "fake", matchIfMissing = true)
@Slf4j
public class FakeAiClient implements AiClient {
    @Override
    public AiResponse chat(AiRequest request) {
        log.info("FakeAiClient processing request with {} messages", request.messages().size());
        String lastUserMessage = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .reduce((a, b) -> b)
            .map(AiRequest.Message::content)
            .orElse("");
        String response = generateFakeResponse(lastUserMessage);
        return new AiResponse(response, 100, 50, "fake/model", null);
    }

    private String generateFakeResponse(String input) {
        if (input.toLowerCase().contains("summar")) {
            return "[FAKE AI] Ticket summary: This is a simulated summary of the support ticket. The issue appears to be operational in nature and requires follow-up with the field team.";
        }
        if (input.toLowerCase().contains("next action") || input.toLowerCase().contains("suggest")) {
            return "[FAKE AI] Suggested next action: Contact the assigned operator to gather additional details and update the ticket status accordingly.";
        }
        if (input.toLowerCase().contains("reply") || input.toLowerCase().contains("draft")) {
            return "[FAKE AI] Draft reply: Dear customer, thank you for contacting us. We have reviewed your request and our team is actively working on a resolution. We will update you shortly.";
        }
        if (input.toLowerCase().contains("missing") || input.toLowerCase().contains("information")) {
            return "[FAKE AI] Missing information:\n- Customer contact details\n- Exact time of incident\n- Affected location or route";
        }
        if (input.toLowerCase().contains("classify") || input.toLowerCase().contains("priority")) {
            return "[FAKE AI] Priority: MEDIUM, Category: OTHER, Reason: Based on the description, this appears to be a standard operational issue.";
        }
        return "[FAKE AI] I understand your request. This is a simulated AI response for development and testing purposes.";
    }
}
