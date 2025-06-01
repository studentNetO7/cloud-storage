package com.semenova.cloudstorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semenova.cloudstorage.dto.EditFileNameRequest;
import com.semenova.cloudstorage.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FileControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private String setupFile(String filename) throws Exception {
        // Копируем тестовый файл
        Path source = Paths.get("src/test/resources/test-files/example.txt");
        Path target = Paths.get("/test-storage", filename);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        // Генерируем UUID для файла
        UUID fileId = UUID.randomUUID();

        // Получаем ID пользователя testuser из базы
        UUID userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                new Object[]{"testuser"},
                UUID.class);

        // Вставляем запись в таблицу files
        jdbcTemplate.update(
                "INSERT INTO files (id, user_id, filename, size, upload_date, path, is_deleted) " +
                        "VALUES (?, ?, ?, ?, NOW(), ?, FALSE)",
                fileId, userId, filename, 123L, target.toString()
        );

        return filename;
    }

    private void cleanupFile(String filename) throws Exception {
        Files.deleteIfExists(Paths.get("/test-storage", filename));
        jdbcTemplate.update("DELETE FROM files WHERE filename = ?", filename);
    }

    @Test
    void listFiles_ShouldReturnFileList() throws Exception {
        String filename = "list-" + UUID.randomUUID() + ".txt";
        setupFile(filename);

        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].filename").value(org.hamcrest.Matchers.hasItem(filename)));

        cleanupFile(filename);
    }

    @Test
    void downloadFile_ShouldReturnFileData() throws Exception {
        String filename = "download-" + UUID.randomUUID() + ".txt";
        setupFile(filename);

        mockMvc.perform(get("/file")
                        .param("filename", filename)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + filename + "\""));

        cleanupFile(filename);
    }

    @Test
    void editFileName_ShouldReturnSuccess() throws Exception {
        String originalName = "edit-" + UUID.randomUUID() + ".txt";
        String newName = "edited-" + UUID.randomUUID() + ".txt";
        setupFile(originalName);

        EditFileNameRequest editRequest = new EditFileNameRequest();
        editRequest.setFilename(newName);

        mockMvc.perform(put("/file")
                        .param("filename", originalName)
                        .header("auth-token", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File name updated successfully"));

        mockMvc.perform(get("/file")
                        .param("filename", newName)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + newName + "\""));

        cleanupFile(newName);
    }

    @Test
    void deleteFile_ShouldReturnSuccess() throws Exception {
        String filename = "delete-" + UUID.randomUUID() + ".txt";
        setupFile(filename);

        mockMvc.perform(delete("/file")
                        .param("filename", filename)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File deleted successfully"));

        mockMvc.perform(get("/file")
                        .param("filename", filename)
                        .header("auth-token", authToken))
                .andExpect(status().is4xxClientError());

        cleanupFile(filename); // На всякий случай
    }
}

