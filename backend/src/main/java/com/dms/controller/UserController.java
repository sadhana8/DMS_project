package com.dms.controller;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.UserRepository;
import com.dms.service.impl.AuthServiceImpl;
import com.dms.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-profile and user-management operations.
 *
 * <p>
 * Base path: {@code /api/users}
 *
 * <h2>No hard deletion</h2>
 * The {@code DELETE /{id}} endpoint no longer exists. Use
 * {@code PUT /{id}/deprecate} instead to soft-deprecate a user account. The
 * account can be restored at any time via {@code PUT /{id}/restore}.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final AuthServiceImpl authService;
    private final UserRepository userRepository;

    /**
     * Returns the full profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails ud) {
        var user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(authService.mapUserToResponse(user));
    }

    /**
     * Updates the authenticated user's own profile (partial update).
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(ud.getUsername(), request));
    }

    /**
     * Returns a paginated list of all <em>active</em> users. Deprecated users
     * are excluded; use {@code GET /admin/deprecated/users} to see them.
     *
     * <p>
     * {@code GET /api/users?page=0&size=10&search=alice}
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.listUsers(page, size, search));
    }

    /**
     * Returns a single user's profile by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    /**
     * Replaces the complete set of roles assigned to a user.
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable Long id,
            @RequestBody UpdateRolesRequest request) {
        return ResponseEntity.ok(userService.updateRoles(id, request.getRoles()));
    }

    /**
     * Re-enables a temporarily deactivated user account.
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User activated"));
    }

    /**
     * Temporarily disables login without deprecating the account.
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated"));
    }

    /**
     * <b>Deprecates</b> a user — the safe, reversible alternative to deletion.
     *
     * <p>
     * {@code PUT /api/users/{id}/deprecate}
     *
     * <p>
     * The account is hidden from all standard queries, login is blocked, and
     * all refresh tokens are revoked. All data (documents, permissions, audit
     * history) is fully preserved. Use {@code PUT /{id}/restore} to undo.
     *
     * <p>
     * Request body:
     * <pre>{@code { "reason": "Left the organisation" }}</pre>
     *
     * @param id the user ID to deprecate
     * @param request optional reason for the deprecation
     * @param ud the authenticated admin user
     * @return {@code 200 OK} with the updated {@link UserResponse}
     */
    @PutMapping("/{id}/deprecate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deprecateUser(
            @PathVariable Long id,
            @RequestBody(required = false) DeprecateRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(userService.deprecateUser(id, reason, ud.getUsername()));
    }

    /**
     * <b>Restores</b> a deprecated user back to active status.
     *
     * <p>
     * {@code PUT /api/users/{id}/restore}
     *
     * <p>
     * Resets {@link com.dms.entity.DeprecationStatus} to ACTIVE, re-enables
     * login, and clears all deprecation audit fields.
     *
     * @param id the user ID to restore
     * @return {@code 200 OK} with the restored {@link UserResponse}
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.restoreUser(id));
    }
}
