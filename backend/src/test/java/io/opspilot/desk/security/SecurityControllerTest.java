package io.opspilot.desk.security;

import io.opspilot.desk.config.JwtProperties;
import io.opspilot.desk.entity.Role;
import io.opspilot.desk.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityControllerTest {

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-must-be-at-least-32-characters-long");
        props.setExpirationMs(3600000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAndValidateToken() {
        User user = testUser();
        String token = jwtService.generate(user);
        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
    }

    @Test
    void invalidToken_returnsFalse() {
        assertThat(jwtService.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void tamperedToken_returnsFalse() {
        User user = testUser();
        String token = jwtService.generate(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    private User testUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hash")
                .fullName("Test")
                .role(Role.OPERATOR)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
