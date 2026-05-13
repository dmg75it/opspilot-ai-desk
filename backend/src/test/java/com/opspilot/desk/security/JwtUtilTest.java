package com.opspilot.desk.security;

import com.opspilot.desk.entity.User;
import com.opspilot.desk.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    private User testUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashed")
                .fullName("Test User")
                .role(Role.OPERATOR)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken(testUser());
        assertThat(token).isNotBlank();
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken(testUser());
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        String token = jwtUtil.generateToken(testUser());
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        User user = testUser();
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com");
    }
}
