package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    /** Purpose: LOGIN_2FA | ENABLE_2FA | SENSITIVE_ACTION. Optional, defaults to SENSITIVE_ACTION. */
    private String purpose;
}
