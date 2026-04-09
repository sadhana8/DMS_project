package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for {@code PUT /api/auth/change-password}.
 *
 * <p>
 * Used by an authenticated user to change their own password. The current
 * password is required for verification before the change is applied.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ChangePasswordRequest {

    /**
     * The user's current plain-text password. Verified against the stored
     * BCrypt hash before accepting the change.
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * The desired new password. Must be at least 8 characters.
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
