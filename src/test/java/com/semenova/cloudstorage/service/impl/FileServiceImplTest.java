package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.exception.FileAlreadyExistsException;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class FileServiceImplTest {

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private FileRepository fileRepository;

    private final User testUser = new User();

    @BeforeEach
    void setUp() {
        fileService.setUploadDir("uploads_test");
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testUser");
    }

    @Test
    void uploadFile_success() throws IOException {
        String filename = "file.txt";

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", filename, "text/plain", "Hello, World!".getBytes());

        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, testUser)).thenReturn(Optional.empty());

        Path testPath = Paths.get(fileService.getUploadDir(), filename);
        Files.deleteIfExists(testPath);

        fileService.uploadFile(testUser, filename, multipartFile);

        assertTrue(Files.exists(testPath));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository, times(1)).save(fileCaptor.capture());

        File savedFile = fileCaptor.getValue();
        assertEquals(filename, savedFile.getFilename());
        assertEquals(testUser, savedFile.getUser());
        assertEquals(multipartFile.getSize(), savedFile.getSize());
        assertFalse(savedFile.getIsDeleted());

        Files.deleteIfExists(testPath);
    }

    @Test
    void uploadFile_fileAlreadyExistsInDb_throwsException() {
        String filename = "file.txt";

        File existingFile = new File();
        existingFile.setFilename(filename);
        existingFile.setUser(testUser);
        existingFile.setIsDeleted(false);

        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, testUser)).thenReturn(Optional.of(existingFile));

        MockMultipartFile multipartFile = new MockMultipartFile("file", filename, "text/plain", "data".getBytes());

        FileAlreadyExistsException ex = assertThrows(FileAlreadyExistsException.class,
                () -> fileService.uploadFile(testUser, filename, multipartFile));
        assertEquals("File with this name already exists", ex.getMessage());
    }

    @Test
    void deleteFile_success() {
        String filename = "file.txt";

        File file = new File();
        file.setFilename(filename);
        file.setUser(testUser);
        file.setIsDeleted(false);

        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, testUser)).thenReturn(Optional.of(file));

        fileService.deleteFile(testUser, filename);

        assertTrue(file.getIsDeleted());
        verify(fileRepository).save(file);
    }

    @Test
    void downloadFile_success() throws IOException {
        String filename = "file.txt";
        Path filePath = Paths.get(fileService.getUploadDir(), filename);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "file content".getBytes());

        File file = new File();
        file.setFilename(filename);
        file.setUser(testUser);
        file.setIsDeleted(false);
        file.setPath(filePath.toString());

        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, testUser)).thenReturn(Optional.of(file));

        byte[] content = fileService.downloadFile(testUser, filename);
        assertArrayEquals("file content".getBytes(), content);

        Files.deleteIfExists(filePath);
    }

    @Test
    void editFileName_success() throws IOException {
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        Path oldPath = Paths.get(fileService.getUploadDir(), oldFilename);
        Path newPath = Paths.get(fileService.getUploadDir(), newFilename);

        Files.createDirectories(oldPath.getParent());
        Files.write(oldPath, "content".getBytes());

        File file = new File();
        file.setFilename(oldFilename);
        file.setUser(testUser);
        file.setIsDeleted(false);
        file.setPath(oldPath.toString());

        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(newFilename, testUser)).thenReturn(Optional.empty());
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(oldFilename, testUser)).thenReturn(Optional.of(file));

        fileService.editFileName(testUser, oldFilename, newFilename);

        assertFalse(Files.exists(oldPath));
        assertTrue(Files.exists(newPath));
        assertEquals(newFilename, file.getFilename());
        assertEquals(newPath.toString(), file.getPath());

        Files.deleteIfExists(newPath);
    }

    @Test
    void listFiles_success() {
        File file1 = new File();
        file1.setFilename("file1.txt");
        file1.setUser(testUser);
        file1.setIsDeleted(false);

        File file2 = new File();
        file2.setFilename("file2.txt");
        file2.setUser(testUser);
        file2.setIsDeleted(false);

        when(fileRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(List.of(file1, file2));

        List<File> files = fileService.listFiles(testUser, 10);

        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.getFilename().equals("file1.txt")));
    }
}
