package io.opspilot.desk.dto.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, String role, boolean active) {}
