package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    String externalRef,
    String title,
    String description,
    String status,
    String priority,
    String category,
    UserDto assignedTo,
    UserDto createdBy,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    Long version
) {}
