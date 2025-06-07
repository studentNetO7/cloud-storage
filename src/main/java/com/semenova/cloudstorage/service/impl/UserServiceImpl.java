package com.semenova.cloudstorage.service.impl;

import com.semenova.cloudstorage.model.User;
import com.semenova.cloudstorage.repository.UserRepository;
import com.semenova.cloudstorage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            logger.debug("User '{}' found in the database", username);
        } else {
            logger.warn("User '{}' not found in the database", username);
        }
        return userOpt;
    }

    @Override
    public boolean authenticate(String username, String rawPassword) {
        Optional<User> userOpt = findByUsername(username); // переиспользование метода выше

        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: user '{}' not found", username);
            return false;
        }

        User user = userOpt.get();
        boolean matches = passwordEncoder.matches(rawPassword, user.getPasswordHash());

        if (matches) {
            logger.info("User '{}' authenticated successfully", username);
        } else {
            logger.warn("Authentication failed: wrong password for user '{}'", username);
        }

        return matches;
    }
}

