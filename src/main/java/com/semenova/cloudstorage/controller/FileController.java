package com.semenova.cloudstorage.controller;

import com.semenova.cloudstorage.dto.EditFileNameRequest;
import com.semenova.cloudstorage.dto.FileResponse;
import com.semenova.cloudstorage.dto.MessageResponse;
import com.semenova.cloudstorage.exception.ResourceNotFoundException;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.UserRepository;
import com.semenova.cloudstorage.service.FileService;
import com.semenova.cloudstorage.util.JwtTokenUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("")
@Validated
public class FileController {

    private final FileService fileService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public FileController(FileService fileService, JwtTokenUtil jwtTokenUtil, UserRepository userRepository) {
        this.fileService = fileService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    private User getUserFromToken(String token) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @PostMapping("/file")
    public ResponseEntity<MessageResponse> uploadFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        fileService.uploadFile(getUserFromToken(token), filename, file);
        return ResponseEntity.ok(new MessageResponse("File uploaded successfully"));
    }

    @DeleteMapping("/file")
    public ResponseEntity<MessageResponse> deleteFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename) {

        fileService.deleteFile(getUserFromToken(token), filename);
        return ResponseEntity.ok(new MessageResponse("File deleted successfully"));
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename) {

        byte[] fileData = fileService.downloadFile(getUserFromToken(token), filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileData);
    }

    @PutMapping("/file")
    public ResponseEntity<MessageResponse> editFileName(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename,
            @Valid @RequestBody EditFileNameRequest request) {

        fileService.editFileName(getUserFromToken(token), filename, request.getFilename());
        return ResponseEntity.ok(new MessageResponse("File name updated successfully"));
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> listFiles(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("limit") @Min(1) int limit) {

        List<File> files = fileService.listFiles(getUserFromToken(token), limit);
        List<FileResponse> responseList = files.stream()
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}
