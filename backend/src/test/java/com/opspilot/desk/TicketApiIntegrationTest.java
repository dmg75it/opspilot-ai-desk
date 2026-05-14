package com.opspilot.desk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.desk.dto.AuthRequest;
import com.opspilot.desk.dto.AuthResponse;
import com.opspilot.desk.dto.TicketRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TicketApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("opspilot_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("ai.provider", () -> "fake");
    }

    @BeforeAll
    static void checkDocker() {
        try {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Skipping: Docker not available in this environment");
        } catch (Exception e) {
            assumeTrue(false, "Skipping: Docker not accessible - " + e.getMessage());
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void loginAndCreateTicket() throws Exception {
        // Login
        String loginBody = objectMapper.writeValueAsString(
            new AuthRequest("operator@example.com", "operator123"));
        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String token = authResponse.token();

        // Create ticket
        String ticketBody = objectMapper.writeValueAsString(
            new TicketRequest(null, "Test Ticket", "Test description for integration test", "MEDIUM", "DELIVERY"));
        mockMvc.perform(post("/api/tickets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test Ticket"))
            .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void unauthorizedAccess_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_operatorForbidden() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
            new AuthRequest("operator@example.com", "operator123"));
        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + authResponse.token()))
            .andExpect(status().isForbidden());
    }
}
