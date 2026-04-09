package com.dms.exception;

/**
 * Thrown when a token (password-reset or refresh) is invalid, expired, or has
 * already been used.
 *
 * <p>
 * Mapped to HTTP {@code 401 Unauthorized} by
 * {@link GlobalExceptionHandler#handleInvalidToken}.
 *
 * <p>
 * Common usage scenarios:
 * <ul>
 * <li>A password-reset link is clicked after the token has expired (60 min
 * default).</li>
 * <li>A password-reset token is submitted a second time after already being
 * used.</li>
 * <li>A refresh token has been revoked (e.g. after logout or password
 * change).</li>
 * <li>A refresh token is not found in the database (possible tampering).</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * Constructs a new exception with a descriptive message.
     *
     * @param message a human-readable description of why the token is invalid
     * (e.g. {@code "Reset token has expired or already been used"})
     */
    public InvalidTokenException(String message) {
        super(message);
    }
}
