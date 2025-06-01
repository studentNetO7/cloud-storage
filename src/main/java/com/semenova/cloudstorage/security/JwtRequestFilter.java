package com.semenova.cloudstorage.security;

import com.semenova.cloudstorage.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);


    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtRequestFilter(JwtTokenUtil jwtTokenUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("auth-token");

        if (token != null) {
            boolean blacklisted = tokenBlacklistService.isTokenBlacklisted(token);
            logger.info("FILTER: blacklist contains = {}", blacklisted);
            boolean valid = jwtTokenUtil.validateToken(token);
            logger.info("Token valid: {}, Token blacklisted: {}", valid, blacklisted);

            if (valid && !blacklisted) {
                var userId = jwtTokenUtil.getUserIdFromToken(token);
                logger.info("Authenticated userId from token: {}", userId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                if (!valid) {
                    logger.warn("Invalid auth-token");
                }
                if (blacklisted) {
                    logger.warn("auth-token is blacklisted");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        } else {
            logger.debug("No auth-token header present in request");
        }
        chain.doFilter(request, response);
    }
}
