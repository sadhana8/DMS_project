package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for {@code POST /api/auth/refresh-token}.
 *
 * <p>
 * Carries the opaque refresh-token string that was issued during login or the
 * previous refresh call. The token is consumed (revoked) and a new
 * access/refresh pair is returned.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class RefreshTokenRequest {

    /**
     * The UUID refresh-token string stored by the client after login. Must
     * match an active, unrevoked, and unexpired
     * {@link com.dms.entity.RefreshToken} row.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
