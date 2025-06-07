package com.semenova.cloudstorage.controller;

import com.semenova.cloudstorage.dto.LoginRequest;
import com.semenova.cloudstorage.dto.LoginResponse;
import com.semenova.cloudstorage.security.TokenBlacklistService;
import com.semenova.cloudstorage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("")
public class AuthController {
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

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
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = request.getHeader("auth-token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "auth-token is missing"));
        }

        tokenBlacklistService.blacklistToken(token);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
