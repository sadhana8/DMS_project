package com.dms.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception-to-HTTP-response mapping for the entire REST API.
 *
 * <p>
 * Annotated with {@link RestControllerAdvice} so it intercepts exceptions
 * thrown from any {@code @RestController} or {@code @Controller} in the
 * application context and converts them into a consistent JSON error structure.
 *
 * <h2>Error response structure</h2>
 * Every error response is serialised from the private {@link ErrorResponse}
 * record:
 * <pre>{@code
 * {
 *   "status":      404,
 *   "error":       "Not Found",
 *   "message":     "Document not found: 42",
 *   "timestamp":   "2024-03-15T10:30:00",
 *   "fieldErrors": null          // only present for validation failures
 * }
 * }</pre>
 *
 * <h2>Exception-to-status mapping</h2>
 * <table border="1">
 * <tr><th>Exception</th><th>HTTP status</th></tr>
 * <tr><td>{@link ResourceNotFoundException}</td><td>404 Not Found</td></tr>
 * <tr><td>{@link DuplicateResourceException}</td><td>409 Conflict</td></tr>
 * <tr><td>{@link InvalidTokenException}</td><td>401 Unauthorized</td></tr>
 * <tr><td>{@link BadCredentialsException}</td><td>401 Unauthorized</td></tr>
 * <tr><td>{@link DisabledException}</td><td>403 Forbidden</td></tr>
 * <tr><td>{@link AccessDeniedException}</td><td>403 Forbidden</td></tr>
 * <tr><td>{@link FileStorageException}</td><td>500 Internal Server
 * Error</td></tr>
 * <tr><td>{@link MaxUploadSizeExceededException}</td><td>413 Payload Too
 * Large</td></tr>
 * <tr><td>{@link MethodArgumentNotValidException}</td><td>400 Bad
 * Request</td></tr>
 * <tr><td>Any other {@link Exception}</td><td>500 Internal Server
 * Error</td></tr>
 * </table>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Immutable value type used as the body for every error response.
     *
     * @param status the numeric HTTP status code
     * @param error a short, standard HTTP status phrase
     * @param message a developer-readable explanation of what went wrong
     * @param timestamp the server time at which the error occurred
     * @param fieldErrors per-field validation messages; {@code null} for
     * non-validation errors
     */
    record ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp,
            Map<String, String> fieldErrors) {

    }

    /**
     * Builds a simple {@link ErrorResponse} without per-field details.
     *
     * @param status numeric HTTP status
     * @param error standard status phrase
     * @param message developer-readable detail
     * @return a fully-populated {@link ErrorResponse}
     */
    private ErrorResponse build(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), null);
    }

    /**
     * Builds an {@link ErrorResponse} that also carries per-field validation
     * errors.
     *
     * @param status numeric HTTP status
     * @param error standard status phrase
     * @param message developer-readable summary
     * @param fieldErrors map of field name → constraint violation message
     * @return a fully-populated {@link ErrorResponse}
     */
    private ErrorResponse build(int status, String error, String message,
            Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), fieldErrors);
    }

    // ── Domain exceptions ─────────────────────────────────────────────────
    /**
     * Handles requests for entities that do not exist in the database.
     *
     * @param e the thrown exception carrying the "not found" message
     * @return a 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException e) {
        return build(404, "Not Found", e.getMessage());
    }

    /**
     * Handles attempts to create a resource that violates a uniqueness
     * constraint.
     *
     * @param e the thrown exception carrying the conflict description
     * @return a 409 error response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateResourceException e) {
        return build(409, "Conflict", e.getMessage());
    }

    /**
     * Handles expired, already-used, or not-found tokens (password-reset,
     * refresh).
     *
     * @param e the thrown exception with a token-specific message
     * @return a 401 error response
     */
    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException e) {
        return build(401, "Unauthorized", e.getMessage());
    }

    // ── Spring Security exceptions ────────────────────────────────────────
    /**
     * Handles failed login attempts where the supplied credentials are wrong.
     * The message is intentionally generic to avoid leaking information about
     * which field (email vs password) was incorrect.
     *
     * @param e the Spring Security bad-credentials exception (message
     * suppressed)
     * @return a 401 error response with a safe generic message
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCreds(BadCredentialsException e) {
        return build(401, "Unauthorized", "Invalid email or password");
    }

    /**
     * Handles login attempts by users whose accounts have been deactivated.
     *
     * @param e the Spring Security disabled-account exception
     * @return a 403 error response
     */
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleDisabled(DisabledException e) {
        return build(403, "Forbidden", "Account is deactivated");
    }

    /**
     * Handles access to endpoints or resources the authenticated user is not
     * permitted to reach (role / permission checks).
     *
     * @param e the Spring Security access-denied exception
     * @return a 403 error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException e) {
        return build(403, "Forbidden", "You don't have permission to perform this action");
    }

    // ── Infrastructure exceptions ─────────────────────────────────────────
    /**
     * Handles errors from {@link com.dms.service.FileStorageService} when a
     * file cannot be stored, read, or deleted.
     *
     * @param e the file-storage exception with an I/O-level message
     * @return a 500 error response
     */
    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleFileStorage(FileStorageException e) {
        return build(500, "File Storage Error", e.getMessage());
    }

    /**
     * Handles multipart uploads that exceed the configured size limit
     * ({@code spring.servlet.multipart.max-file-size}, default 50 MB).
     *
     * @param e the Spring size-exceeded exception
     * @return a 413 Payload Too Large error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleMaxUpload(MaxUploadSizeExceededException e) {
        return build(413, "Payload Too Large",
                "File size exceeds the maximum allowed limit of 50 MB");
    }

    // ── Validation ────────────────────────────────────────────────────────
    /**
     * Handles bean-validation failures on {@code @RequestBody} DTOs annotated
     * with {@code @Valid}.
     *
     * <p>
     * Collects every field-level constraint violation into a map and includes
     * it in the {@link ErrorResponse#fieldErrors()} field so the client can
     * display per-field error messages.
     *
     * @param e the validation exception containing one or more binding errors
     * @return a 400 error response with a populated {@code fieldErrors} map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String field = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            errors.put(field, err.getDefaultMessage());
        });
        return build(400, "Validation Failed", "Request validation failed", errors);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────
    /**
     * Safety net for any unexpected exception not caught by a more specific
     * handler.
     *
     * <p>
     * Logs the full stack trace at {@code ERROR} level (important for
     * diagnosing production issues) but returns only a generic message to the
     * client to avoid leaking internal implementation details.
     *
     * @param e the uncaught exception
     * @return a 500 error response with a generic message
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return build(500, "Internal Server Error", "An unexpected error occurred");
    }
}
