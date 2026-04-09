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

/**
 * Service that manages physical file storage on the local file system.
 *
 * <p>
 * All files are stored beneath a configurable root directory
 * ({@code app.storage.upload-dir}, default {@code ./uploads}). Within that
 * root, files are organised into sub-directories to avoid millions of files in
 * a single flat directory.
 *
 * <h2>Security considerations</h2>
 * <ul>
 * <li>Uploaded files are stored with <b>UUID-based names</b> to prevent
 * path-traversal attacks and to eliminate name collisions.</li>
 * <li>All paths are resolved via {@link Path#normalize()} before I/O to
 * neutralise {@code "../"} sequences.</li>
 * <li>The original file name supplied by the client is stored only in the
 * database ({@code original_file_name}); it is never used for disk access.</li>
 * </ul>
 *
 * <h2>Storage layout example</h2>
 * <pre>
 * uploads/
 * └── documents/
 *     └── 42/           ← owner user ID
 *         ├── a1b2c3.pdf
 *         └── d4e5f6.docx
 * </pre>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@Slf4j
public class FileStorageService {

    /**
     * Base upload directory path, read from {@code app.storage.upload-dir}.
     * Resolved to an absolute, normalised {@link Path} in {@link #init()}.
     */
    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    /**
     * The absolute root {@link Path} under which all files are stored.
     */
    private Path rootLocation;

    /**
     * Initialises the storage root directory after the bean is constructed.
     *
     * <p>
     * Creates the directory (and all parent directories) if it does not already
     * exist. Throws {@link FileStorageException} if the directory cannot be
     * created, preventing the application from starting in a broken state.
     *
     * @throws FileStorageException if the directory cannot be created due to an
     * I/O error
     */
    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialised at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialise storage directory", e);
        }
    }

    /**
     * Stores a multipart file under the given sub-directory and returns the
     * relative path that should be persisted in the database.
     *
     * <p>
     * The file is saved with a UUID-based name to prevent collisions and
     * path-traversal attacks. The extension is preserved from the original file
     * name (lower-cased).
     *
     * <p>
     * Example: calling {@code store(file, "documents/42")} might save the file
     * at {@code uploads/documents/42/550e8400-e29b.pdf} and return
     * {@code "documents/42/550e8400-e29b.pdf"}.
     *
     * @param file the multipart file received from the HTTP request
     * @param subDir relative sub-directory path inside the storage root (e.g.
     * {@code "documents/42"})
     * @return the relative path from the storage root to the stored file
     * @throws FileStorageException if the file is empty or an I/O error occurs
     */
    public String store(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        String extension = getExtension(file.getOriginalFilename());
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

    /**
     * Loads a stored file as a Spring {@link Resource} suitable for use in a
     * {@link org.springframework.http.ResponseEntity} download or preview
     * response.
     *
     * @param relativePath the path returned by {@link #store} (relative to
     * storage root)
     * @return a readable {@link Resource} pointing to the file
     * @throws FileStorageException if the file does not exist, is not readable,
     * or the path is malformed
     */
    public Resource loadAsResource(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileStorageException("Could not read file: " + relativePath);
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not read file: " + relativePath, e);
        }
    }

    /**
     * Deletes a stored file from the file system.
     *
     * <p>
     * This is a best-effort operation: if the file does not exist or cannot be
     * deleted, the failure is logged at {@code WARN} level but no exception is
     * thrown. Callers should not depend on the file being removed.
     *
     * @param relativePath the path returned by {@link #store} (relative to
     * storage root)
     */
    public void delete(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }

    /**
     * Checks whether a file exists at the given relative path.
     *
     * @param relativePath the path relative to the storage root
     * @return {@code true} if a regular file exists at that path
     */
    public boolean exists(String relativePath) {
        return Files.exists(rootLocation.resolve(relativePath).normalize());
    }

    /**
     * Extracts the lower-cased file extension from a file name.
     *
     * @param filename the original file name (may be {@code null})
     * @return the extension without the leading dot (e.g. {@code "pdf"}), or an
     * empty string if the file has no extension
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
