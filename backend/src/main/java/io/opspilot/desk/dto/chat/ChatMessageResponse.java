package io.opspilot.desk.dto.chat;

import io.opspilot.desk.entity.ChatMessage;
import io.opspilot.desk.entity.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        MessageRole role,
        String content,
        String model,
        Integer tokenEstimate,
        Instant createdAt,
        boolean errorFlag,
        String errorMessage
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(), m.getSession().getId(), m.getRole(), m.getContent(),
                m.getModel(), m.getTokenEstimate(), m.getCreatedAt(),
                m.isErrorFlag(), m.getErrorMessage()
        );
    }
}
