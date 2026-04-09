package com.dms.dto.response;

import lombok.*;

/**
 * Standard API response wrapper for simple success/failure messages.
 *
 * <p>
 * Used for endpoints that do not return a domain object (e.g. logout, activate
 * user, delete document). The {@link #success} flag allows clients to
 * differentiate without inspecting the HTTP status code.
 *
 * <p>
 * Factory methods {@link #ok} and {@link #error} avoid boilerplate at call
 * sites.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {

    /**
     * {@code true} if the operation completed successfully.
     */
    private boolean success;

    /**
     * A human-readable message describing the outcome.
     */
    private String message;

    /**
     * Creates a successful response with the given message.
     *
     * @param message the success description
     * @return an {@code ApiResponse} with {@code success = true}
     */
    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }

    /**
     * Creates an error response with the given message.
     *
     * @param message the error description
     * @return an {@code ApiResponse} with {@code success = false}
     */
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
}
