package io.opspilot.desk.dto.ai;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    String role,
    String content,
    String model,
    Integer promptTokens,
    Integer completionTokens,
    Instant createdAt,
    boolean error,
    String errorMessage) {}
