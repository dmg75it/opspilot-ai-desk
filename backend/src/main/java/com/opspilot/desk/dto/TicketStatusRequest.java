package com.opspilot.desk.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketStatusRequest(@NotBlank String status) {}
