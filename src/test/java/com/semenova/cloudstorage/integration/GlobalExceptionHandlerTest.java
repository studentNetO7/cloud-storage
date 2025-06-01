package com.semenova.cloudstorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semenova.cloudstorage.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void login() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "testpassword");

        String responseBody = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(responseBody).get("auth-token").asText();
        assertThat(authToken).isNotBlank();
    }

    @Test
    @DisplayName("Должен вернуть 400 Bad Request при ошибке валидации")
    void shouldReturn400WhenValidationFails() throws Exception {
        Map<String, String> invalidRequest = Map.of("value", ""); // value не должен быть пустым

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("auth-token", authToken)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Error input data"));
    }

    @Test
    @DisplayName("Должен вернуть 500 Internal Server Error при RuntimeException")
    void shouldReturn500WhenUnhandledExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/internal-error")
                        .header("auth-token", authToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Error upload file"));
    }
}
