package io.opspilot.desk.dto.note;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(UUID id, UUID ticketId, String authorEmail, String body, String visibility, Instant createdAt) {}
