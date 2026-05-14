package com.opspilot.desk.service;

import com.opspilot.desk.dto.AuthRequest;
import com.opspilot.desk.dto.AuthResponse;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import com.opspilot.desk.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AppUser user = userRepository.findByEmail(request.email()).orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));
        log.info("User logged in: {}", request.email());
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
