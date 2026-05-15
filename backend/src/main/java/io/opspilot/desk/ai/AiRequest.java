package io.opspilot.desk.ai;

import java.util.List;

public record AiRequest(String systemPrompt, List<Message> messages) {
    public record Message(String role, String content) {}
}
