package com.opspilot.desk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
    String externalRef,
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotNull String priority,
    @NotNull String category
) {}
