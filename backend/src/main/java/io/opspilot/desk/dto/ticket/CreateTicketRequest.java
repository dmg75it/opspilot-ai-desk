package io.opspilot.desk.dto.ticket;

import jakarta.validation.constraints.*;

public record CreateTicketRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotBlank String priority,
    @NotBlank String category,
    String externalRef) {}
