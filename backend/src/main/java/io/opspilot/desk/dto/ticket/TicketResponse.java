package io.opspilot.desk.dto.ticket;

import io.opspilot.desk.dto.auth.UserResponse;
import io.opspilot.desk.entity.*;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String externalRef,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        UserResponse assignedTo,
        UserResponse createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Long version
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(), t.getExternalRef(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getPriority(), t.getCategory(),
                t.getAssignedTo() != null ? UserResponse.from(t.getAssignedTo()) : null,
                UserResponse.from(t.getCreatedBy()),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(),
                t.getVersion()
        );
    }
}
