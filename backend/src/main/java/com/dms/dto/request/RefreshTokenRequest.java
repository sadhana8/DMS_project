package com.dms.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
