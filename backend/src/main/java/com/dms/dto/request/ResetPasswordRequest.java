package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for {@code POST /api/auth/reset-password}.
 *
 * <p>
 * Carries the one-time token from the reset link and the user's chosen new
 * password.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ResetPasswordRequest {

    /**
     * The opaque UUID token extracted from the password-reset link query
     * parameter. Must match an active, unexpired, and unused
     * {@link com.dms.entity.PasswordResetToken}.
     */
    @NotBlank(message = "Reset token is required")
    private String token;

    /**
     * The user's desired new password. Must be at least 8 characters;
     * BCrypt-hashed before storage.
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
