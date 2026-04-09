package com.dms.exception;

/**
 * Thrown when an attempt is made to create a resource that would violate a
 * uniqueness constraint.
 *
 * <p>
 * Mapped to HTTP {@code 409 Conflict} by
 * {@link GlobalExceptionHandler#handleDuplicate}.
 *
 * <p>
 * Common usage scenarios:
 * <ul>
 * <li>Registering a new user with an e-mail that already exists.</li>
 * <li>Registering a new user with a username that is already taken.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new exception with a descriptive message.
     *
     * @param message a human-readable description of the conflict (e.g.
     * {@code "Email already registered"})
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
