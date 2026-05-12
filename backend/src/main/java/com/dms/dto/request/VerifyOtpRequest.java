package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4,8}", message = "OTP must be 4-8 digits")
    private String code;

    /** Purpose: LOGIN_2FA | ENABLE_2FA | SENSITIVE_ACTION. Optional, defaults to SENSITIVE_ACTION. */
    private String purpose;
}
