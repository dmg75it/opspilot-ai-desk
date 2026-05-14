package com.opspilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {JwtService.class})
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    JwtService jwtService;

    private UserDetails userDetails(String email) {
        return User.builder()
            .username(email)
            .password("ignored")
            .roles("OPERATOR")
            .build();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        var token = jwtService.generateToken(userDetails("test@example.com"));
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsEmail() {
        var token = jwtService.generateToken(userDetails("user@test.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_withMatchingUser_returnsTrue() {
        var ud = userDetails("valid@test.com");
        var token = jwtService.generateToken(ud);
        assertThat(jwtService.isTokenValid(token, ud)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_returnsFalse() {
        var token = jwtService.generateToken(userDetails("a@test.com"));
        assertThat(jwtService.isTokenValid(token, userDetails("b@test.com"))).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        try {
            var token = jwtService.generateToken(userDetails("expired@test.com"));
            assertThat(jwtService.isTokenValid(token, userDetails("expired@test.com"))).isFalse();
        } finally {
            ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
        }
    }
}
