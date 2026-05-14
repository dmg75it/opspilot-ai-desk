package com.opspilot.service;

import com.opspilot.dto.request.LoginRequest;
import com.opspilot.dto.response.AuthResponse;
import com.opspilot.dto.response.UserResponse;
import com.opspilot.repository.UserRepository;
import com.opspilot.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            log.warn("AUTH login failure: email={}", request.email());
            throw ex;
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        var user = userRepository.findByEmail(request.email()).orElseThrow();
        log.info("AUTH login success: email={} role={}", user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public UserResponse getCurrentUser(String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName());
    }
}
