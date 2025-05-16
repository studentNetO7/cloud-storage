package com.semenova.cloudstorage.controller;

import com.semenova.cloudstorage.dto.EditFileNameRequest;
import com.semenova.cloudstorage.dto.FileResponse;
import com.semenova.cloudstorage.dto.MessageResponse;
import com.semenova.cloudstorage.exception.ResourceNotFoundException;
import com.semenova.cloudstorage.service.FileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cloud")
@Validated
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/file")
    public ResponseEntity<MessageResponse> uploadFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename,
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        try {
            fileService.uploadFile(token, filename, file);
            return ResponseEntity.ok(new MessageResponse("File uploaded successfully"));
        } catch (Exception ex) {
            throw new RuntimeException("Error uploading file: " + ex.getMessage());
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<MessageResponse> deleteFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename) {

        try {
            fileService.deleteFile(token, filename);
            return ResponseEntity.ok(new MessageResponse("File deleted successfully"));
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename) {

        byte[] fileData;
        try {
            fileData = fileService.downloadFile(token, filename);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileData);
    }

    @PutMapping("/file")
    public ResponseEntity<MessageResponse> editFileName(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("filename") @NotBlank String filename,
            @Valid @RequestBody EditFileNameRequest request) {

        try {
            fileService.editFileName(token, filename, request.getFilename());
            return ResponseEntity.ok(new MessageResponse("File name updated successfully"));
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> listFiles(
            @RequestHeader("auth-token") @NotBlank String token,
            @RequestParam("limit") @Min(1) int limit) {

        return ResponseEntity.ok(fileService.listFiles(token, limit));
    }
}

