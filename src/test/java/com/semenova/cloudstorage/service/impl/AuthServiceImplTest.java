package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.security.TokenBlacklistService;
import com.semenova.cloudstorage.service.UserService;
import com.semenova.cloudstorage.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_validCredentials_returnsToken() {
        String login = "testuser";
        String password = "password";
        String expectedToken = "token123";

        User mockUser = new User();
        mockUser.setUsername(login);
        mockUser.setPasswordHash("hashed");

        when(userService.authenticate(login, password)).thenReturn(true);
        when(userService.findByUsername(login)).thenReturn(Optional.of(mockUser));
        when(jwtTokenUtil.generateToken(mockUser)).thenReturn(expectedToken);

        String actualToken = authService.login(login, password);

        assertEquals(expectedToken, actualToken);
        verify(userService).authenticate(login, password);
        verify(userService).findByUsername(login);
        verify(jwtTokenUtil).generateToken(mockUser);
    }

    @Test
    void login_invalidCredentials_throwsException() {
        String login = "testuser";
        String password = "wrong";

        when(userService.authenticate(login, password)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(login, password);
        });

        verify(userService).authenticate(login, password);
        verifyNoMoreInteractions(userService, jwtTokenUtil);
    }

    @Test
    void logout_validToken_addsToBlacklist() {
        String token = "valid.jwt.token";

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);

        authService.logout(token);

        verify(jwtTokenUtil).validateToken(token);
        verify(tokenBlacklistService).isTokenBlacklisted(token);
        verify(tokenBlacklistService).blacklistToken(token);
    }

    @Test
    void logout_invalidToken_throwsException() {
        String token = "invalid.jwt.token";

        when(jwtTokenUtil.validateToken(token)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            authService.logout(token);
        });

        verify(jwtTokenUtil).validateToken(token);
        verifyNoMoreInteractions(tokenBlacklistService);
    }

    @Test
    void logout_tokenAlreadyBlacklisted_doesNothing() {
        String token = "already.blacklisted.token";

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

        authService.logout(token);

        verify(jwtTokenUtil).validateToken(token);
        verify(tokenBlacklistService).isTokenBlacklisted(token);
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }
}
