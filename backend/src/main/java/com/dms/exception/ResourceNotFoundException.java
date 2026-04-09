package com.dms.exception;

/**
 * Thrown when a requested resource cannot be found in the database.
 *
 * <p>
 * Mapped to HTTP {@code 404 Not Found} by
 * {@link GlobalExceptionHandler#handleNotFound}.
 *
 * <p>
 * Common usage scenarios:
 * <ul>
 * <li>A document, user, or folder with the given ID does not exist.</li>
 * <li>A user lookup by e-mail returns no result.</li>
 * <li>A specific document version or permission entry is missing.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with a descriptive message.
     *
     * @param message a human-readable description indicating which resource was
     * not found (e.g. {@code "Document not found: 42"})
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
