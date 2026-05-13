package com.opspilot.desk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.desk.dto.auth.LoginRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Helper for obtaining JWT tokens in integration tests.
 */
final class TokenHelper {

    private TokenHelper() {}

    static String obtainToken(MockMvc mockMvc, ObjectMapper objectMapper,
                              String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
