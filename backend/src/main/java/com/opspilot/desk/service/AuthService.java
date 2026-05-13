package com.opspilot.desk.service;

import com.opspilot.desk.dto.auth.AuthResponse;
import com.opspilot.desk.dto.auth.LoginRequest;
import com.opspilot.desk.dto.auth.UserProfileResponse;
import com.opspilot.desk.entity.User;
import com.opspilot.desk.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = (User) auth.getPrincipal();
        String token = jwtUtil.generateToken(user);
        log.info("User authenticated email={}", user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    public UserProfileResponse getProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
