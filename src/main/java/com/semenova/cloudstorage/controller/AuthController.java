package com.semenova.cloudstorage.controller;

import com.semenova.cloudstorage.dto.LoginRequest;
import com.semenova.cloudstorage.dto.LoginResponse;
import com.semenova.cloudstorage.dto.MessageResponse;
import com.semenova.cloudstorage.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cloud")
public class AuthController {

    private final AuthService authService;

    // Конструктор с внедрением зависимости
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Метод для авторизации пользователя и получения токена
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.getLogin(), loginRequest.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    // Метод для выхода пользователя из системы
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("auth-token") String token) {
        authService.logout(token);
        return ResponseEntity.ok(new MessageResponse("Successfully logged out"));
    }
}
