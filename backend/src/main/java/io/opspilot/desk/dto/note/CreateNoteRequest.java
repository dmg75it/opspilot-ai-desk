package io.opspilot.desk.dto.note;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(@NotBlank String body) {}
