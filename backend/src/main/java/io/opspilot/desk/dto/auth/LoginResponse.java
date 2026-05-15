package io.opspilot.desk.dto.auth;

public record LoginResponse(String token, UserResponse user) {}
