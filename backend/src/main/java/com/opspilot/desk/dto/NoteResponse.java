package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    UUID ticketId,
    String authorName,
    String body,
    String visibility,
    Instant createdAt
) {}
