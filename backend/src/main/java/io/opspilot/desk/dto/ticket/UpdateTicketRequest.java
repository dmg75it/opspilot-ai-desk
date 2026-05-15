package io.opspilot.desk.dto.ticket;

import io.opspilot.desk.entity.TicketCategory;
import io.opspilot.desk.entity.TicketPriority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        String externalRef,
        @Size(max = 150) String title,
        @Size(max = 5000) String description,
        TicketPriority priority,
        TicketCategory category
) {}
