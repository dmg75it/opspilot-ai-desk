package io.opspilot.desk.service;

import io.opspilot.desk.dto.auth.LoginRequest;
import io.opspilot.desk.dto.auth.LoginResponse;
import io.opspilot.desk.dto.auth.UserResponse;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = (User) auth.getPrincipal();
        String token = jwtService.generate(user);
        log.info("Login successful: email={} role={}", user.getEmail(), user.getRole());
        return new LoginResponse(token, UserResponse.from(user));
    }
}
