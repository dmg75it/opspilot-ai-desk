package io.opspilot.desk.dto.auth;

public record LoginResponse(String token, String email, String role) {}
