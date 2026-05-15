package io.opspilot.desk.dto.ticket;

import io.opspilot.desk.entity.TicketCategory;
import io.opspilot.desk.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        String externalRef,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull TicketPriority priority,
        @NotNull TicketCategory category
) {}
