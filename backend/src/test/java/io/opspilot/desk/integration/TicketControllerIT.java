package io.opspilot.desk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("opspilot")
        .withUsername("opspilot")
        .withPassword("opspilot");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String operatorToken;
    static String createdTicketId;

    @Test @Order(1)
    void loginAsOperator() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"operator@example.com\",\"password\":\"operator123\"}"))
            .andExpect(status().isOk()).andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        operatorToken = body.get("token").asText();
    }

    @Test @Order(2)
    void createTicket() throws Exception {
        var result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test ticket\",\"description\":\"Test description\",\"priority\":\"HIGH\",\"category\":\"DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test ticket"))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdTicketId = body.get("id").asText();
    }

    @Test @Order(3)
    void listTickets() throws Exception {
        mockMvc.perform(get("/api/tickets")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test @Order(4)
    void changeTicketStatus() throws Exception {
        mockMvc.perform(post("/api/tickets/" + createdTicketId + "/status")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @Order(5)
    void invalidStatusTransitionReturns422() throws Exception {
        mockMvc.perform(post("/api/tickets/" + createdTicketId + "/status")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"NEW\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
