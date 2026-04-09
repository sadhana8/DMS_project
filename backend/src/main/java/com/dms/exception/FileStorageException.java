package com.dms.exception;

/**
 * Thrown when a file cannot be stored, retrieved, or deleted from the
 * underlying storage system.
 *
 * <p>
 * Mapped to HTTP {@code 500 Internal Server Error} by
 * {@link GlobalExceptionHandler#handleFileStorage}.
 *
 * <p>
 * Common usage scenarios:
 * <ul>
 * <li>The upload-directory does not exist and cannot be created.</li>
 * <li>An uploaded file is empty (zero bytes).</li>
 * <li>A stored file cannot be located when building a download response.</li>
 * <li>An {@link java.io.IOException} occurs while copying the multipart stream
 * to disk.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.service.FileStorageService
 */
public class FileStorageException extends RuntimeException {

    /**
     * Constructs a new exception with a descriptive message and no cause.
     *
     * @param message a human-readable description of the storage failure
     */
    public FileStorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a descriptive message and the root cause.
     *
     * @param message a human-readable description of the storage failure
     * @param cause the underlying {@link Throwable} that triggered this
     * exception
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
