package com.semenova.cloudstorage.service;

import com.semenova.cloudstorage.dto.FileResponse;
import com.semenova.cloudstorage.model.File;
import com.semenova.cloudstorage.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface FileService {

    void uploadFile(String token, String filename, MultipartFile file);

    void deleteFile(String token, String filename);

    byte[] downloadFile(String token, String filename);

    void editFileName(String token, String oldFilename, String newFilename);

    List<FileResponse> listFiles(String token, int limit);
}
