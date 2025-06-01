package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.security.TokenBlacklistService;
import com.semenova.cloudstorage.service.AuthService;
import com.semenova.cloudstorage.service.UserService;
import com.semenova.cloudstorage.util.JwtTokenUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(UserService userService,
                           JwtTokenUtil jwtTokenUtil,
                           TokenBlacklistService tokenBlacklistService) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public String login(String login, String password) {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Login and password must be provided");
        }
        if (!userService.authenticate(login, password)) {
            logger.warn("Login failed: invalid login or password for '{}'", login);
            throw new BadCredentialsException("Invalid login or password");
        }

        User user = userService.findByUsername(login)
                .orElseThrow(() -> new IllegalStateException("User must exist after authentication"));
        logger.info("User '{}' logged in successfully", login);
        return jwtTokenUtil.generateToken(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank() || !jwtTokenUtil.validateToken(token)) {
            logger.warn("Logout failed: invalid token {}", token);
            throw new IllegalArgumentException("Invalid token");
        }

        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            logger.info("Token already blacklisted: {}", token);
            return;
        }
        logger.info("Logging out token: {}", token);

        tokenBlacklistService.blacklistToken(token);
        logger.info("Token added to blacklist: {}", token);
    }
}
