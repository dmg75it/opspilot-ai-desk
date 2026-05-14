package com.opspilot.desk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(@NotBlank @Size(max = 10000) String body) {}
