package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for the {@code POST /api/auth/login} endpoint.
 *
 * <p>
 * Carries the user's e-mail and password. Both fields are validated server-side
 * before the authentication attempt is made.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class LoginRequest {

    /**
     * The user's e-mail address used as their login identifier. Must be a
     * well-formed e-mail address and must not be blank.
     */
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    /**
     * The user's plain-text password. Validated here only for presence;
     * strength requirements are enforced on registration and password-change
     * endpoints.
     */
    @NotBlank(message = "Password is required")
    private String password;
}
