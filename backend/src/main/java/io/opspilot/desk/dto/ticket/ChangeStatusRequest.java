package io.opspilot.desk.dto.ticket;

import io.opspilot.desk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TicketStatus status) {}
