package io.opspilot.desk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.secret = "bXlTdXBlclNlY3JldEtleUZvck9wc1BpbG90QUlEZXNrMjAyNg==";
        jwtService.expirationMs = 86400000L;
    }

    @Test
    void generateAndValidateToken() {
        UserDetails user = User.withUsername("test@example.com")
            .password("x").authorities(List.of()).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void extractUsernameFromToken() {
        UserDetails user = User.withUsername("admin@example.com")
            .password("x").authorities(List.of()).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }
}
