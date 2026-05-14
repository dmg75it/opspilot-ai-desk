package com.opspilot.controller;

import com.opspilot.config.TestcontainersConfiguration;
import com.opspilot.dto.request.LoginRequest;
import com.opspilot.dto.response.AuthResponse;
import com.opspilot.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void login_withValidCredentials_returnsToken() {
        var request = new LoginRequest("admin@example.com", "admin123");
        var response = restTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo("admin@example.com");
        assertThat(response.getBody().role()).isEqualTo("ADMIN");
    }

    @Test
    void login_withInvalidCredentials_returns401() {
        var request = new LoginRequest("admin@example.com", "wrongpassword");
        var response = restTemplate.postForEntity("/api/auth/login", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withOperatorCredentials_returnsToken() {
        var request = new LoginRequest("operator@example.com", "operator123");
        var response = restTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo("OPERATOR");
    }

    @Test
    void me_withValidToken_returnsUserProfile() {
        var loginResp = restTemplate.postForEntity("/api/auth/login",
            new LoginRequest("operator@example.com", "operator123"), AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResp.getBody().token();

        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
            new HttpEntity<>(headers), UserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("operator@example.com");
        assertThat(response.getBody().role()).isEqualTo("OPERATOR");
    }

    @Test
    void me_withoutToken_returns401() {
        var response = restTemplate.getForEntity("/api/auth/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withEmptyEmail_returns400() {
        var request = new LoginRequest("", "password");
        var response = restTemplate.postForEntity("/api/auth/login", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
