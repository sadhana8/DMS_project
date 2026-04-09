package com.dms.dto.response;

import lombok.*;

/**
 * Response DTO returned by login, register, and refresh-token endpoints.
 *
 * <p>
 * Contains everything the frontend needs to authenticate subsequent requests:
 * the short-lived access token, the long-lived refresh token, and the user's
 * profile so the UI can be populated without an extra {@code /me} call.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Short-lived JWT to include in the {@code Authorization: Bearer} header of
     * every subsequent API request. Default lifetime: 24 hours.
     */
    private String accessToken;

    /**
     * Long-lived opaque UUID token used to obtain a new access token without
     * re-entering credentials. Default lifetime: 7 days. Must be stored
     * securely by the client (e.g. HttpOnly cookie or secure storage).
     */
    private String refreshToken;

    /**
     * Always {@code "Bearer"} — indicates the token scheme expected by the API.
     */
    private String tokenType;

    /**
     * The authenticated user's full profile, including assigned roles.
     * Populated immediately after login or registration to avoid a round-trip
     * to {@code GET /api/users/me}.
     */
    private UserResponse user;
}
