package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
    UUID id,
    UUID ticketId,
    Instant createdAt
) {}
