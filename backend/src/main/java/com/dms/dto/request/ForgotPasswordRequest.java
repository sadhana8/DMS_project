package com.dms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for {@code POST /api/auth/forgot-password}.
 *
 * <p>
 * Carries only the e-mail address of the account for which a reset link should
 * be generated and sent.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ForgotPasswordRequest {

    /**
     * The e-mail address of the account requesting a password reset. Must be a
     * well-formed e-mail; the service returns silently if no account exists
     * with this address (anti-enumeration).
     */
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;
}
