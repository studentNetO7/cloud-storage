package com.semenova.cloudstorage.security;

import com.semenova.cloudstorage.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class TokenBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // synchronizedSet для потокобезопасности
    private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

    // Добавляет токен в черный список.
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or empty");
        }
        String cleanToken = JwtTokenUtil.sanitizeToken(token);
        blacklistedTokens.add(cleanToken);
        logger.info("Token added to blacklist: [{}]", cleanToken);
    }

    // Проверяет, находится ли токен в черном списке.
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String cleanToken = JwtTokenUtil.sanitizeToken(token);
        boolean contains = blacklistedTokens.contains(cleanToken);
        logger.info("Checking if token is blacklisted: [{}] => {}", cleanToken, contains);
        return contains;
    }
}
