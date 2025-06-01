package com.semenova.cloudstorage.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void blacklistToken_addsTokenToBlacklist() {
        String token = "sample-token";

        tokenBlacklistService.blacklistToken(token);

        assertTrue(tokenBlacklistService.isTokenBlacklisted(token), "Token should be blacklisted");
    }

    @Test
    void isTokenBlacklisted_returnsFalseForNonBlacklistedToken() {
        String token = "not-blacklisted-token";

        assertFalse(tokenBlacklistService.isTokenBlacklisted(token), "Token should not be blacklisted");
    }

    @Test
    void blacklistToken_allowsMultipleTokens() {
        String token1 = "token1";
        String token2 = "token2";

        tokenBlacklistService.blacklistToken(token1);
        tokenBlacklistService.blacklistToken(token2);

        assertTrue(tokenBlacklistService.isTokenBlacklisted(token1), "Token1 should be blacklisted");
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token2), "Token2 should be blacklisted");
    }
}
