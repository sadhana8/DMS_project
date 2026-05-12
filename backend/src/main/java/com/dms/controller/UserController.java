package com.dms.controller;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.UserRepository;
import com.dms.service.impl.AuthServiceImpl;
import com.dms.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user management.
 *
 * <p>Endpoint summary:
 * <ul>
 *   <li>{@code GET  /api/users/me}                   – current user</li>
 *   <li>{@code PUT  /api/users/profile}              – edit own profile</li>
 *   <li>{@code GET  /api/users}                      – paged list (HR+)</li>
 *   <li>{@code GET  /api/users/directory}            – compact "employee" list (HR+)</li>
 *   <li>{@code GET  /api/users/deprecated}           – deprecated users (ADMIN)</li>
 *   <li>{@code GET  /api/users/{id}}                 – single user (HR+)</li>
 *   <li>{@code PUT  /api/users/{id}/roles}           – change roles (ADMIN)</li>
 *   <li>{@code PUT  /api/users/{id}/activate}        – restore / activate (ADMIN)</li>
 *   <li>{@code PUT  /api/users/{id}/deactivate}      – deprecate (ADMIN)</li>
 *   <li>{@code POST /api/users/{id}/deprecate}       – deprecate with optional reason (ADMIN)</li>
 *   <li>{@code POST /api/users/{id}/restore}         – restore (ADMIN)</li>
 *   <li>{@code DELETE /api/users/{id}}               – permanent delete (ADMIN)</li>
 * </ul>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final AuthServiceImpl authService;
    private final UserRepository  userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails ud) {
        var user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(authService.mapUserToResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(ud.getUsername(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String search) {
        return ResponseEntity.ok(userService.listUsers(page, size, search));
    }

    /**
     * Compact list of all users, used by the audit-filter dropdown and the
     * sharing autocomplete. Minimal payload, no paging.
     */
    @GetMapping("/directory")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<Map<String, Object>>> directory() {
        return ResponseEntity.ok(userService.directory());
    }

    @GetMapping("/deprecated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> listDeprecated(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.listDeprecated(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable Long id,
            @RequestBody UpdateRolesRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(userService.updateRoles(id, request.getRoles(), ud.getUsername()));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User activated"));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated"));
    }

    @PostMapping("/{id}/deprecate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deprecate(
            @PathVariable Long id,
            @RequestBody(required = false) DeprecateRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        userService.deprecate(id, req != null ? req.getReason() : null, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User deprecated"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        userService.restore(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User restored"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        userService.deleteUser(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User permanently deleted"));
    }

    /**
     * Admin creates a new user. Generates a random password, sends it via email,
     * and forces the new user to change it on first login.
     */
    @PostMapping("/admin-create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> adminCreate(
            @Valid @RequestBody com.dms.dto.request.AdminCreateUserRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(userService.adminCreate(req, ud.getUsername()));
    }

    /**
     * Suspend a user — immediate access revocation + force logout.
     * Unlike terminate, this sets no termination timestamp and sends no
     * termination email. It is fully reversible via /restore.
     * Reason is required.
     */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> suspend(
            @PathVariable Long id,
            @Valid @RequestBody com.dms.dto.request.TerminationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        userService.suspend(id, req.getReason(), ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User suspended — login blocked and sessions revoked"));
    }

    /**
     * Terminate a user — immediate access revocation. Reason is required.
     */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> terminate(
            @PathVariable Long id,
            @Valid @RequestBody com.dms.dto.request.TerminationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        userService.terminate(id, req.getReason(), ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User terminated, access revoked"));
    }

    /**
     * Employee resigns themselves. Effective date defaults to last day of the
     * current month at 23:59:59. Body may include a reason.
     */
    @PostMapping("/me/resign")
    public ResponseEntity<ApiResponse> resignSelf(
            @RequestBody(required = false) com.dms.dto.request.ResignationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        var user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userService.resign(user.getId(),
                req != null ? req : new com.dms.dto.request.ResignationRequest(),
                ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
            "Resignation recorded. Your access will be revoked at end of the resignation period."));
    }

    /**
     * Admin records a resignation on behalf of an employee. May override the
     * effective date.
     */
    @PostMapping("/{id}/resign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> resignOnBehalf(
            @PathVariable Long id,
            @Valid @RequestBody com.dms.dto.request.ResignationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        userService.resign(id, req, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Resignation recorded"));
    }
}
