package com.dms.controller;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing all authentication and credential-management
 * endpoints.
 *
 * <p>
 * Base path: {@code /api/auth}
 *
 * <p>
 * All endpoints are publicly accessible (no JWT required) except
 * {@code /logout} and {@code /change-password}, which require an authenticated
 * user represented by a valid JWT in the {@code Authorization: Bearer} header.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see AuthServiceImpl
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Delegate for all authentication and credential business logic.
     */
    private final AuthServiceImpl authService;

    /**
     * Authenticates a user with e-mail and password and issues JWT tokens.
     *
     * <p>
     * {@code POST /api/auth/login}
     *
     * <p>
     * Request body example:
     * <pre>{@code
     * { "email": "user@example.com", "password": "secret123" }
     * }</pre>
     *
     * @param request the login credentials; validated by {@code @Valid}
     * @return {@code 200 OK} with an {@link AuthResponse} containing
     * {@code accessToken}, {@code refreshToken}, and user profile data
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Creates a new user account and returns JWT tokens so the user is
     * immediately authenticated after registration.
     *
     * <p>
     * {@code POST /api/auth/register}
     *
     * @param request the registration payload; validated by {@code @Valid}
     * @return {@code 201 Created} with an {@link AuthResponse}
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Issues a new access/refresh token pair by consuming a valid refresh
     * token. The submitted token is immediately revoked (single-use rotation).
     *
     * <p>
     * {@code POST /api/auth/refresh-token}
     *
     * @param request wraps the {@code refreshToken} string
     * @return {@code 200 OK} with a new {@link AuthResponse}
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * Logs the authenticated user out by revoking all their active refresh
     * tokens. The short-lived access token must be discarded by the client.
     *
     * <p>
     * {@code POST /api/auth/logout}
     *
     * @param userDetails Spring Security principal resolved from the JWT filter
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    /**
     * Initiates the forgot-password flow. A one-time reset link is sent to the
     * address if an account exists. Returns the same response either way to
     * prevent user-enumeration attacks.
     *
     * <p>
     * {@code POST /api/auth/forgot-password}
     *
     * @param request contains the e-mail address; validated by {@code @Valid}
     * @return {@code 200 OK} with a generic confirmation message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account with that email exists, a reset link has been sent"));
    }

    /**
     * Completes the password-reset flow by validating the one-time token and
     * persisting the new password.
     *
     * <p>
     * {@code POST /api/auth/reset-password}
     *
     * @param request contains the {@code token} from the e-mail link and the
     * {@code newPassword}; validated by {@code @Valid}
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    /**
     * Allows an authenticated user to change their own password after verifying
     * the current one.
     *
     * <p>
     * {@code PUT /api/auth/change-password}
     *
     * @param userDetails the authenticated user
     * @param request contains {@code currentPassword} and {@code newPassword};
     * validated by {@code @Valid}
     * @return {@code 200 OK} with a success message
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }
}
