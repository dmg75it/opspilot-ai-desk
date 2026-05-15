package io.opspilot.desk.service;

import io.opspilot.desk.dto.auth.*;
import io.opspilot.desk.repository.UserRepository;
import io.opspilot.desk.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        log.info("Login successful for {}", request.email());
        var userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        var user = userRepository.findByEmail(request.email()).orElseThrow();
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }

    public UserResponse currentUser(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isActive());
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
            .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.isActive()))
            .toList();
    }
}
