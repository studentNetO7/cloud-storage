package com.semenova.cloudstorage.service;

public interface AuthService {
    String login(String login, String password);
    void logout(String token);
    boolean isTokenBlacklisted(String token);
}
