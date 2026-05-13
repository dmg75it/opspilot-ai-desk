package com.opspilot.desk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.desk.dto.ticket.ChangeStatusRequest;
import com.opspilot.desk.dto.ticket.CreateTicketRequest;
import com.opspilot.desk.entity.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getOperatorToken() throws Exception {
        return TokenHelper.obtainToken(mockMvc, objectMapper, "operator@example.com", "operator123");
    }

    @Test
    void createTicket_returns201() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Delivery issue at depot A");
        request.setDescription("Package stuck at depot A for 3 days without status update.");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + getOperatorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void listTickets_returns200WithPagination() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + getOperatorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void changeStatus_validTransition_returns200() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("Status change test ticket");
        createRequest.setDescription("This ticket will have its status changed in a test.");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + getOperatorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        ChangeStatusRequest statusRequest = new ChangeStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + getOperatorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_invalidTransition_returns400() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("Invalid transition test ticket");
        createRequest.setDescription("This ticket will have an invalid status transition attempted.");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + getOperatorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        ChangeStatusRequest statusRequest = new ChangeStatusRequest();
        statusRequest.setStatus(TicketStatus.RESOLVED);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + getOperatorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest());
    }
}
