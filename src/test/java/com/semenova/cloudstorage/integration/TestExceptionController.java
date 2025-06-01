package com.semenova.cloudstorage.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Validated
public class TestExceptionController {

    @Setter
    @Getter
    public static class TestRequest {
        @NotBlank(message = "Field 'value' must not be blank")
        private String value;

    }

    // Эндпоинт для проверки 400 BAD REQUEST при валидации
    @PostMapping("/validate")
    public ResponseEntity<String> validateRequest(@Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok("Valid request");
    }

    // Эндпоинт для проверки 500 INTERNAL SERVER ERROR
    @GetMapping("/internal-error")
    public ResponseEntity<String> internalError() {
        throw new RuntimeException("Test internal server error");
    }
}
