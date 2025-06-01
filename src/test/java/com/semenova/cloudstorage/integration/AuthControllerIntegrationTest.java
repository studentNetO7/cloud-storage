package com.semenova.cloudstorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semenova.cloudstorage.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "testpassword");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['auth-token']").isNotEmpty());
    }

    @Test
    void login_invalidCredentials_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("wrongLogin", "wrongPassword");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error input data"));
    }

    @Test
    @DisplayName("После logout токен должен быть недействительным и возвращать 401 при доступе к /list")
    void logout_validTokenThenAccess_returnsUnauthorized() throws Exception {
        // 1. Авторизация — получаем валидный токен
        LoginRequest loginRequest = new LoginRequest("testuser", "testpassword");

        String responseBody = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String authToken = objectMapper.readTree(responseBody).get("auth-token").asText();
        assertThat(authToken).isNotBlank();

        // 2. Логаут — отправляем токен
        mockMvc.perform(post("/logout")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // 3. Попытка использовать этот же токен для доступа к защищенному ресурсу /list с параметром limit
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10")) // обязательный параметр limit
                .andExpect(status().isUnauthorized());
    }
}
