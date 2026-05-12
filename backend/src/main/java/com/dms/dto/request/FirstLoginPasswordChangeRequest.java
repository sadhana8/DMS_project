package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for {@code POST /api/auth/first-login-password-change}. Used the very
 * first time an admin-created account logs in. Doesn't require the current
 * password (a temp one was emailed) but does require the new one to meet the
 * complexity rules.
 */
@Data
public class FirstLoginPasswordChangeRequest {

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
