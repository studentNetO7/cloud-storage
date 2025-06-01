package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.dto.FileResponse;
import com.semenova.cloudstorage.exception.FileAlreadyExistsException;
import com.semenova.cloudstorage.exception.FileStorageException;
import com.semenova.cloudstorage.exception.ResourceNotFoundException;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.FileRepository;
import com.semenova.cloudstorage.repository.UserRepository;
import com.semenova.cloudstorage.service.FileService;
import com.semenova.cloudstorage.util.JwtTokenUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Setter
    @Getter
    @Value("${file.upload-dir}")
    private String uploadDir;


    @Autowired
    public FileServiceImpl(FileRepository fileRepository, UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostConstruct
    public void init() {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                logger.info("Upload directory created at: " + uploadDir);
            } catch (IOException e) {
                logger.error("Failed to create upload directory", e);
                throw new RuntimeException("Could not create upload directory", e);
            }
        } else {
            logger.info("Upload directory already exists at: " + uploadDir);
        }
    }

    @Override
    public void uploadFile(String token, String filename, MultipartFile file) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Проверка: если файл уже есть в базе (и не удалён)
        Optional<File> existingFile = fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user);
        if (existingFile.isPresent()) {
            logger.warn("Upload failed: file '{}' already exists in database for user '{}'", filename, user.getUsername());
            throw new FileAlreadyExistsException("File with this name already exists");
        }

        Path filePath = Paths.get(uploadDir).resolve(filename);

        // Проверка: если физический файл уже существует
        if (Files.exists(filePath)) {
            logger.warn("Upload failed: file physically exists on disk at '{}'", filePath);
            throw new FileAlreadyExistsException("File already exists on disk");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
            logger.info("File '{}' saved to disk at '{}'", filename, filePath);
        } catch (IOException e) {
            logger.error("Failed to save file '{}' to disk at '{}'", filename, filePath, e);
            throw new FileStorageException("Failed to save file to disk", e); //
        }

        // Сохранение метаданных в БД
        File newFile = new File();
        newFile.setUser(user);
        newFile.setFilename(filename);
        newFile.setSize(file.getSize());
        newFile.setUploadDate(java.time.LocalDateTime.now());
        newFile.setPath(filePath.toString());
        newFile.setIsDeleted(false);

        fileRepository.save(newFile);
        logger.info("File '{}' uploaded and saved to DB for user '{}'", filename, user.getUsername());
    }


    @Override
    public void deleteFile(String token, String filename) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)
                .ifPresentOrElse(file -> {
                    file.setIsDeleted(true);
                    fileRepository.save(file);
                    logger.info("File '{}' deleted for user '{}'", filename, user.getUsername());
                }, () -> {
                    throw new ResourceNotFoundException("File not found");
                });
    }

    @Override
    public byte[] downloadFile(String token, String filename) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        File file = fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        try {
            Path filePath = Paths.get(file.getPath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            logger.error("Error reading file '{}'", filename, e);
            throw new FileStorageException("Error reading file", e);
        }
    }

    @Override
    public void editFileName(String token, String oldFilename, String newFilename) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Проверяем, существует ли уже файл с новым именем у пользователя и не удалён ли он
        boolean newNameExists = fileRepository.findByFilenameAndUserAndIsDeletedFalse(newFilename, user).isPresent();
        if (newNameExists) {
            throw new FileAlreadyExistsException("File with name '" + newFilename + "' already exists");
        }

        fileRepository.findByFilenameAndUserAndIsDeletedFalse(oldFilename, user)
                .ifPresentOrElse(file -> {
                    Path oldPath = Paths.get(file.getPath());
                    Path newPath = Paths.get(uploadDir).resolve(newFilename);

                    if (Files.exists(newPath)) {
                        throw new FileAlreadyExistsException("File with name '" + newFilename + "' already exists on disk");
                    }

                    try {
                        // Переименовать файл на диске
                        Files.move(oldPath, newPath);

                        // Обновить данные в БД
                        file.setFilename(newFilename);
                        file.setPath(newPath.toString());
                        fileRepository.save(file);

                        logger.info("File '{}' renamed to '{}' for user '{}'", oldFilename, newFilename, user.getUsername());
                    } catch (IOException e) {
                        logger.error("Failed to rename file on disk", e);
                        throw new FileStorageException("Failed to rename file on disk", e);
                    }
                }, () -> {
                    throw new ResourceNotFoundException("File not found");
                });
    }

    @Override
    public List<FileResponse> listFiles(String token, int limit) {
        UUID userId = jwtTokenUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return fileRepository.findByUserAndIsDeletedFalse(user).stream()
                .limit(limit)
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .toList();
    }
}
