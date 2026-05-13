package com.opspilot.desk.ai;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FakeAiChatProvider implements AiChatProvider {

    @Override
    public AiResponse chat(String systemPrompt, List<AiMessage> messages) {
        log.info("Fake AI responding to message count={}", messages.size());

        String lastUserMessage = messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.getRole()))
                .reduce((a, b) -> b)
                .map(AiMessage::getContent)
                .orElse("");

        String content = generateResponse(lastUserMessage);
        return AiResponse.builder()
                .content(content)
                .model("fake-ai")
                .tokenCount(content.split("\\s+").length)
                .build();
    }

    private String generateResponse(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("summarize") || lower.contains("summary")) {
            return "This ticket has been submitted and is currently under review. The issue involves an operational matter requiring attention from the support team.";
        }
        if (lower.contains("suggest") || lower.contains("reply")) {
            return "Thank you for contacting us. We are looking into your issue and will update you shortly.";
        }
        if (lower.contains("classify") || lower.contains("priority")) {
            return "Based on the description, this appears to be a MEDIUM priority ticket in the DELIVERY category.";
        }
        return "I understand your request. Let me help you with this ticket. Could you provide more details about the specific issue you are experiencing?";
    }
}
