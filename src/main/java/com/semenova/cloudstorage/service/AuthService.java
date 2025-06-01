package com.semenova.cloudstorage.service;

public interface AuthService {
    String login(String email, String password);

    void logout(String token);
}
