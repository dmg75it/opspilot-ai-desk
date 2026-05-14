package com.opspilot.desk.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(@NotBlank String content) {}
