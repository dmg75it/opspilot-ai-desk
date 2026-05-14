package com.opspilot.desk.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "fake", matchIfMissing = true)
@Slf4j
public class FakeAiProvider implements AiProvider {

    @Override
    public AiResponse chat(String systemPrompt, List<AiMessage> messages) {
        log.info("[FAKE AI] Processing {} messages", messages.size());
        String lastUserMessage = messages.stream()
            .filter(m -> "user".equalsIgnoreCase(m.role()))
            .reduce((a, b) -> b)
            .map(AiMessage::content)
            .orElse("");

        String response = generateFakeResponse(lastUserMessage);
        return AiResponse.success(response, getModelName(), response.length() / 4);
    }

    @Override
    public String getModelName() {
        return "fake-ai-v1";
    }

    private String generateFakeResponse(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("summarize") || lower.contains("summary")) {
            return "This ticket describes an operational issue requiring attention. The operator has reported a problem that needs to be investigated and resolved. Key details are available in the ticket description.";
        }
        if (lower.contains("suggest") || lower.contains("next action")) {
            return "Suggested next actions: 1) Review the ticket details carefully. 2) Contact the customer for additional information. 3) Escalate if needed. 4) Update the ticket status accordingly.";
        }
        if (lower.contains("reply") || lower.contains("draft")) {
            return "Dear Customer, thank you for reaching out. We have received your request and our team is currently investigating the issue. We will provide an update within 24 hours. Best regards, Support Team.";
        }
        if (lower.contains("priority") || lower.contains("classify")) {
            return "Based on the ticket content, this appears to be a MEDIUM priority issue in the DELIVERY category. The issue should be addressed within the standard SLA window.";
        }
        if (lower.contains("missing") || lower.contains("information")) {
            return "The following information appears to be missing: 1) Exact date/time of the issue. 2) Order/shipment reference number. 3) Customer contact details. 4) Steps already taken to resolve.";
        }
        return "I understand your request. Based on the ticket information provided, I recommend reviewing the current status and ensuring all relevant details are documented. Please let me know if you need specific assistance.";
    }
}
