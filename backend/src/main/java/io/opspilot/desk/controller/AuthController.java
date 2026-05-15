package io.opspilot.desk.controller;

import io.opspilot.desk.dto.auth.LoginRequest;
import io.opspilot.desk.dto.auth.LoginResponse;
import io.opspilot.desk.dto.auth.UserResponse;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }
}
