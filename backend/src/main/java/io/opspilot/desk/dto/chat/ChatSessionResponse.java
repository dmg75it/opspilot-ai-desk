package io.opspilot.desk.dto.chat;

import io.opspilot.desk.entity.ChatSession;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(UUID id, UUID ticketId, Instant createdAt, Instant updatedAt) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getTicket().getId(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
