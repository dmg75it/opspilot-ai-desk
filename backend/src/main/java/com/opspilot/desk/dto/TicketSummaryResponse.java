package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record TicketSummaryResponse(
    UUID id,
    String externalRef,
    String title,
    String status,
    String priority,
    String category,
    String assignedToName,
    String createdByName,
    Instant createdAt,
    Instant updatedAt
) {}
