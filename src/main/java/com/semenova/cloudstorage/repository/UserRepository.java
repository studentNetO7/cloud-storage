package com.semenova.cloudstorage.repository;

import com.semenova.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

      //Поиск пользователя по username (вход выполняется по email, равному username).

    Optional<User> findByUsername(String username);
}
