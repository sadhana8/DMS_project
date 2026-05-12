package com.dms.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;

    /** When true, the client must send the user to a password-change screen. */
    @Builder.Default
    private Boolean mustChangePassword = false;
}
