package io.opspilot.desk.dto.ticket;

import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
    @Size(max = 150) String title,
    @Size(max = 5000) String description,
    String priority, String category, String externalRef) {}
