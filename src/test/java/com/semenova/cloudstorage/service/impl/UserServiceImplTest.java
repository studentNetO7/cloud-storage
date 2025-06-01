package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findByUsername_userExists_returnsUser() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByUsername_userNotFound_returnsEmpty() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername(username);

        assertFalse(result.isPresent());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void authenticate_validCredentials_returnsTrue() {
        String username = "testuser";
        String rawPassword = "password";
        String hashedPassword = "hashedPassword";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

        boolean result = userService.authenticate(username, rawPassword);

        assertTrue(result);
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(rawPassword, hashedPassword);
    }

    @Test
    void authenticate_wrongPassword_returnsFalse() {
        String username = "testuser";
        String rawPassword = "wrong";
        String hashedPassword = "correctHash";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false);

        boolean result = userService.authenticate(username, rawPassword);

        assertFalse(result);
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(rawPassword, hashedPassword);
    }

    @Test
    void authenticate_userNotFound_returnsFalse() {
        String username = "unknown";
        String rawPassword = "any";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        boolean result = userService.authenticate(username, rawPassword);

        assertFalse(result);
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(passwordEncoder);
    }
}
