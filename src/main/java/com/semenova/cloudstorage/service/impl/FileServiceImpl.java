package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.exception.FileAlreadyExistsException;
import com.semenova.cloudstorage.exception.FileStorageException;
import com.semenova.cloudstorage.exception.ResourceNotFoundException;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.FileRepository;
import com.semenova.cloudstorage.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository fileRepository;

    @Setter
    @Getter
    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                logger.info("Upload directory created at: {}", uploadDir);
            } catch (IOException e) {
                logger.error("Failed to create upload directory", e);
                throw new RuntimeException("Could not create upload directory", e);
            }
        } else {
            logger.info("Upload directory already exists at: {}", uploadDir);
        }
    }

    @Override
    @Transactional
    public void uploadFile(User user, String filename, MultipartFile file) {
        logger.info("Uploading file '{}' for user '{}'", filename, user.getUsername());

        // Проверка на наличие файла в БД
        Optional<File> existingFile = fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user);
        if (existingFile.isPresent()) {
            logger.warn("File '{}' already exists for user '{}'", filename, user.getUsername());
            throw new FileAlreadyExistsException("File with this name already exists");
        }

        // Проверка на наличие файла на диске
        Path finalPath = Paths.get(uploadDir).resolve(filename);
        if (Files.exists(finalPath)) {
            logger.warn("File '{}' already exists on disk", filename);
            throw new FileAlreadyExistsException("File already exists on disk");
        }

        Path tempFile = null;

        try (var inputStream = file.getInputStream()) {
            // 1. Сохраняем временный файл
            tempFile = Files.createTempFile("upload_", "_" + filename);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 2. Создаем и сохраняем запись в БД
            File newFile = new File();
            newFile.setUser(user);
            newFile.setFilename(filename);
            newFile.setSize(file.getSize());
            newFile.setUploadDate(LocalDateTime.now());
            newFile.setPath(finalPath.toString());
            newFile.setIsDeleted(false);

            fileRepository.save(newFile);

            // 3. Перемещаем временный файл на целевое место
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("File '{}' saved to DB and disk", filename);

        } catch (IOException e) {
            logger.error("Error during file upload for '{}'", filename, e);
            throw new FileStorageException("Failed to upload file", e);
        } finally {
            // 4. Удаляем временный файл, если он остался
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupError) {
                    logger.warn("Failed to delete temp file: {}", tempFile, cleanupError);
                }
            }
        }
    }


    @Override
    public void deleteFile(User user, String filename) {
        fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)
                .ifPresentOrElse(file -> {
                    file.setIsDeleted(true);
                    fileRepository.save(file);
                    logger.info("File '{}' marked as deleted for user '{}'", filename, user.getUsername());
                }, () -> {
                    throw new ResourceNotFoundException("File not found");
                });
    }

    @Override
    public byte[] downloadFile(User user, String filename) {
        File file = fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        try {
            logger.info("Downloading file '{}' for user '{}'", filename, user.getUsername());
            return Files.readAllBytes(Paths.get(file.getPath()));
        } catch (IOException e) {
            logger.error("Error reading file '{}'", filename, e);
            throw new FileStorageException("Error reading file", e);
        }
    }

    @Override
    public void editFileName(User user, String oldFilename, String newFilename) {
        boolean newNameExists = fileRepository.findByFilenameAndUserAndIsDeletedFalse(newFilename, user).isPresent();
        if (newNameExists) {
            throw new FileAlreadyExistsException("File with this name already exists");
        }

        fileRepository.findByFilenameAndUserAndIsDeletedFalse(oldFilename, user)
                .ifPresentOrElse(file -> {
                    Path oldPath = Paths.get(file.getPath());
                    Path newPath = Paths.get(uploadDir).resolve(newFilename);

                    if (Files.exists(newPath)) {
                        throw new FileAlreadyExistsException("File with this name already exists on disk");
                    }

                    try {
                        Files.move(oldPath, newPath);
                        file.setFilename(newFilename);
                        file.setPath(newPath.toString());
                        fileRepository.save(file);
                        logger.info("File '{}' renamed to '{}'", oldFilename, newFilename);
                    } catch (IOException e) {
                        throw new FileStorageException("Failed to rename file", e);
                    }
                }, () -> {
                    throw new ResourceNotFoundException("File not found");
                });
    }

    @Override
    public List<File> listFiles(User user, int limit) {
        logger.info("Listing files for user '{}' with limit {}", user.getUsername(), limit);
        return fileRepository.findByUserAndIsDeletedFalse(user)
                .stream()
                .limit(limit)
                .toList();
    }
}
