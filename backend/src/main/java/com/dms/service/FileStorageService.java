package com.dms.service;

import com.dms.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory", e);
        }
    }

    public String store(MultipartFile file, String subDir) {
        if (file.isEmpty()) throw new FileStorageException("Cannot store empty file");

        String extension  = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path targetDir = rootLocation.resolve(subDir);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return subDir + "/" + storedName;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new FileStorageException("Could not read file: " + relativePath);
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not read file: " + relativePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(rootLocation.resolve(relativePath).normalize());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
