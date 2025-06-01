package com.semenova.cloudstorage.util;

import com.semenova.cloudstorage.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenUtil {

    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expirationMs = 24 * 60 * 60 * 1000; // 24 часа


    // Генерация токена для пользователя.
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // UUID пользователя в subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // Извлечение UUID пользователя из JWT.
    public UUID getUserIdFromToken(String token) {
        String cleanToken = sanitizeToken(token);
        return UUID.fromString(
                Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(cleanToken)
                        .getBody()
                        .getSubject()
        );
    }

    // Проверка валидности токена.
    public boolean validateToken(String token) {
        String cleanToken = sanitizeToken(token);
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(cleanToken);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // Очистка токена от префикса "Bearer ", если он есть.
    public static String sanitizeToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7); // Удаляем "Bearer "
        }
        return token;
    }
}
