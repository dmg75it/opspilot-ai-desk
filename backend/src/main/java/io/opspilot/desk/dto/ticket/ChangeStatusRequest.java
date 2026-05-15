package io.opspilot.desk.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatusRequest(@NotBlank String status) {}
