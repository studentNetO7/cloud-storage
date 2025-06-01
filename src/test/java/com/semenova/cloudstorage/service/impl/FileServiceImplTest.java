package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.dto.FileResponse;
import com.semenova.cloudstorage.exception.FileAlreadyExistsException;
import com.semenova.cloudstorage.exception.ResourceNotFoundException;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.FileRepository;
import com.semenova.cloudstorage.repository.UserRepository;
import com.semenova.cloudstorage.util.JwtTokenUtil;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        fileService.setUploadDir("uploads_test");
    }

    @Test
    void uploadFile_success() throws IOException {
        String token = "validToken";
        String filename = "file.txt";

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", filename, "text/plain", "Hello, World!".getBytes());

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.empty());

        Path testPath = Paths.get(fileService.getUploadDir(), filename);
        if (Files.exists(testPath)) {
            Files.delete(testPath);
        }

        fileService.uploadFile(token, filename, multipartFile);

        assertTrue(Files.exists(testPath));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository, times(1)).save(fileCaptor.capture());

        File savedFile = fileCaptor.getValue();
        assertEquals(filename, savedFile.getFilename());
        assertEquals(user, savedFile.getUser());
        assertEquals(multipartFile.getSize(), savedFile.getSize());
        assertFalse(savedFile.getIsDeleted());

        Files.deleteIfExists(testPath);
    }

    @Test
    void uploadFile_fileAlreadyExistsInDb_throwsException() {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        File existingFile = new File();
        existingFile.setFilename(filename);
        existingFile.setUser(user);
        existingFile.setIsDeleted(false);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.of(existingFile));

        MockMultipartFile multipartFile = new MockMultipartFile("file", filename, "text/plain", "data".getBytes());

        FileAlreadyExistsException ex = assertThrows(FileAlreadyExistsException.class,
                () -> fileService.uploadFile(token, filename, multipartFile));
        assertEquals("File with this name already exists", ex.getMessage());
    }

    @Test
    void uploadFile_fileAlreadyExistsOnDisk_throwsException() throws IOException {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.empty());

        Path testPath = Paths.get(fileService.getUploadDir(), filename);
        Files.createDirectories(testPath.getParent());
        Files.write(testPath, "data".getBytes());

        MockMultipartFile multipartFile = new MockMultipartFile("file", filename, "text/plain", "data".getBytes());

        FileAlreadyExistsException ex = assertThrows(FileAlreadyExistsException.class,
                () -> fileService.uploadFile(token, filename, multipartFile));
        assertEquals("File already exists on disk", ex.getMessage());

        Files.deleteIfExists(testPath);
    }

    @Test
    void deleteFile_success() {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        File file = new File();
        file.setFilename(filename);
        file.setUser(user);
        file.setIsDeleted(false);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.of(file));

        fileService.deleteFile(token, filename);

        assertTrue(file.getIsDeleted());
        verify(fileRepository).save(file);
    }

    @Test
    void deleteFile_fileNotFound_throwsException() {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> fileService.deleteFile(token, filename));
        assertEquals("File not found", ex.getMessage());
    }

    @Test
    void downloadFile_success() throws IOException {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Path tempFilePath = Paths.get(fileService.getUploadDir(), filename);
        Files.createDirectories(tempFilePath.getParent());
        Files.write(tempFilePath, "file content".getBytes());

        File file = new File();
        file.setFilename(filename);
        file.setUser(user);
        file.setIsDeleted(false);
        file.setPath(tempFilePath.toString());

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.of(file));

        byte[] content = fileService.downloadFile(token, filename);
        assertArrayEquals("file content".getBytes(), content);

        Files.deleteIfExists(tempFilePath);
    }

    @Test
    void downloadFile_fileNotFound_throwsException() {
        String token = "token";
        String filename = "file.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(filename, user)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> fileService.downloadFile(token, filename));
        assertEquals("File not found", ex.getMessage());
    }

    @Test
    void editFileName_success() throws IOException {
        String token = "token";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Path oldPath = Paths.get(fileService.getUploadDir(), oldFilename);
        Path newPath = Paths.get(fileService.getUploadDir(), newFilename);

        Files.createDirectories(oldPath.getParent());
        Files.write(oldPath, "content".getBytes());

        File file = new File();
        file.setFilename(oldFilename);
        file.setUser(user);
        file.setIsDeleted(false);
        file.setPath(oldPath.toString());

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(newFilename, user)).thenReturn(Optional.empty());
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(oldFilename, user)).thenReturn(Optional.of(file));

        fileService.editFileName(token, oldFilename, newFilename);

        assertFalse(Files.exists(oldPath));
        assertTrue(Files.exists(newPath));

        assertEquals(newFilename, file.getFilename());
        assertEquals(newPath.toString(), file.getPath());

        Files.deleteIfExists(newPath);
    }

    @Test
    void editFileName_newFilenameAlreadyExistsInDb_throwsException() {
        // Arrange
        String token = "token";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        File fileWithNewName = new File();
        fileWithNewName.setFilename(newFilename);
        fileWithNewName.setUser(user);
        fileWithNewName.setIsDeleted(false);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByFilenameAndUserAndIsDeletedFalse(newFilename, user))
                .thenReturn(Optional.of(fileWithNewName));

        // Act & Assert
        FileAlreadyExistsException ex = assertThrows(
                FileAlreadyExistsException.class,
                () -> fileService.editFileName(token, oldFilename, newFilename)
        );

        assertEquals("File with name 'new.txt' already exists", ex.getMessage());
    }


    @Test
    void listFiles_success() {
        String token = "token";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        File file1 = new File();
        file1.setFilename("file1.txt");
        file1.setUser(user);
        file1.setIsDeleted(false);

        File file2 = new File();
        file2.setFilename("file2.txt");
        file2.setUser(user);
        file2.setIsDeleted(false);

        when(jwtTokenUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findByUserAndIsDeletedFalse(user)).thenReturn(Arrays.asList(file1, file2));

        List<FileResponse> files = fileService.listFiles(token, 10);

        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.getFilename().equals("file1.txt")));
        assertTrue(files.stream().anyMatch(f -> f.getFilename().equals("file2.txt")));
    }
}

