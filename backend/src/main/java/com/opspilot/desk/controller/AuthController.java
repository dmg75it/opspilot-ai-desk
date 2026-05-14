package com.opspilot.desk.controller;

import com.opspilot.desk.dto.AuthRequest;
import com.opspilot.desk.dto.AuthResponse;
import com.opspilot.desk.dto.UserDto;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import com.opspilot.desk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserDetails principal) {
        AppUser user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(new UserDto(
            user.getId(), user.getEmail(), user.getFullName(),
            user.getRole().name(), user.isEnabled(), user.getCreatedAt()
        ));
    }
}
