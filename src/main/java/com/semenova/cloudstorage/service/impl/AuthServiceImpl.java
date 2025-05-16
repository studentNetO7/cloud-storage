package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.UserRepository;
import com.semenova.cloudstorage.security.TokenBlacklistService;
import com.semenova.cloudstorage.service.AuthService;
import com.semenova.cloudstorage.util.JwtTokenUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenUtil jwtTokenUtil,
                           TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public String login(String login, String password) {
        User user = userRepository.findByUsername(login)
                .orElseThrow(() -> {
                    logger.warn("Login failed: user '{}' not found", login);
                    return new BadCredentialsException("Invalid login or password");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Login failed: wrong password for user '{}'", login);
            throw new BadCredentialsException("Invalid login or password");
        }

        logger.info("User '{}' logged in successfully", login);
        return jwtTokenUtil.generateToken(user);
    }

    @Override
    public void logout(String token) {
        if (!jwtTokenUtil.validateToken(token)) {
            logger.warn("Logout failed: invalid token {}", token);
            throw new IllegalArgumentException("Invalid token");
        }

        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            logger.info("Token already blacklisted: {}", token);
            return;
        }

        tokenBlacklistService.blacklistToken(token);
        logger.info("Token added to blacklist: {}", token);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.isTokenBlacklisted(token);
    }
}
