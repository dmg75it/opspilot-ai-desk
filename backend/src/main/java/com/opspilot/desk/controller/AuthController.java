package com.opspilot.desk.controller;

import com.opspilot.desk.dto.auth.AuthResponse;
import com.opspilot.desk.dto.auth.LoginRequest;
import com.opspilot.desk.dto.auth.UserProfileResponse;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getProfile(user));
    }
}
