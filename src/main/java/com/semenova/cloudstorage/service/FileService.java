package com.semenova.cloudstorage.service;

import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    void uploadFile(User user, String filename, MultipartFile file);

    void deleteFile(User user, String filename);

    byte[] downloadFile(User user, String filename);

    void editFileName(User user, String oldFilename, String newFilename);

    List<File> listFiles(User user, int limit);
}
