package com.semenova.cloudstorage.service;

import com.semenova.cloudstorage.model.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);

    boolean authenticate(String username, String password);
}
