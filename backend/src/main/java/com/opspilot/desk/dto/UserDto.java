package com.opspilot.desk.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String fullName,
    String role,
    boolean enabled,
    Instant createdAt
) {}
