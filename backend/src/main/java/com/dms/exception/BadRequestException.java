package com.dms.exception;

/**
 * Thrown when a request is well-formed (parsable JSON, all required fields
 * present) but the values are not acceptable — e.g. an email address whose
 * domain does not resolve, a date in the wrong range, etc.
 *
 * <p>
 * The {@link GlobalExceptionHandler} maps this to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
