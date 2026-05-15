package io.opspilot.desk.dto.ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
    UUID id, String externalRef, String title, String description,
    String status, String priority, String category,
    String assignedToEmail, String createdByEmail,
    Instant createdAt, Instant updatedAt, Instant resolvedAt, Long version) {}
