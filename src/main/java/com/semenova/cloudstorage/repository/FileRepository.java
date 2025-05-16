package com.semenova.cloudstorage.repository;

import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByUserAndIsDeletedFalse(User user);
    Optional<File> findByFilenameAndUserAndIsDeletedFalse(String filename, User user);
}
