package com.opspilot.desk.dto;

import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(
    @Size(max = 150) String title,
    @Size(max = 5000) String description,
    String priority,
    String category,
    String externalRef
) {}
