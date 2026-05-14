package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID sessionId,
    String role,
    String content,
    String model,
    Integer tokenEstimate,
    boolean error,
    String errorMessage,
    Instant createdAt
) {}
