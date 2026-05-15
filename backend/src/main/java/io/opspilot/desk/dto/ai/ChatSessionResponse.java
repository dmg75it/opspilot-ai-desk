package io.opspilot.desk.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatSessionResponse(
    UUID id,
    UUID ticketId,
    Instant createdAt,
    List<ChatMessageResponse> messages) {}
