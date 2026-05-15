package io.opspilot.desk.ai;

import java.util.List;

public class FakeAiClient implements AiClient {

    private static final String MODEL = "fake/local";

    @Override
    public AiResponse chat(List<AiMessage> messages) {
        String lastUser = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(AiMessage::content)
                .orElse("");

        String response = buildFakeResponse(lastUser);
        return new AiResponse(response, MODEL, response.length() / 4);
    }

    private String buildFakeResponse(String userMessage) {
        if (userMessage.toLowerCase().contains("summary") || userMessage.toLowerCase().contains("riassunto")) {
            return "This is a fake AI summary. The ticket describes an operational issue that requires attention. " +
                    "Key points: issue reported, awaiting investigation, no resolution yet.";
        }
        if (userMessage.toLowerCase().contains("suggest") || userMessage.toLowerCase().contains("reply")) {
            return "Dear customer, thank you for contacting our support team. " +
                    "We have received your request and are investigating the issue. " +
                    "We will provide an update within 24 hours.";
        }
        if (userMessage.toLowerCase().contains("priority") || userMessage.toLowerCase().contains("category")) {
            return "Based on the description, I suggest: Priority: MEDIUM, Category: OTHER. " +
                    "Please review and adjust as needed.";
        }
        return "This is a fake AI response. In production, configure OPENROUTER_API_KEY and set OPENROUTER_FAKE_MODE=false.";
    }
}
