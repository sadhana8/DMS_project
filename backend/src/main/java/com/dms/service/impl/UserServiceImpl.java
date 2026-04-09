package com.dms.service.impl;

import com.dms.dto.request.UpdateProfileRequest;
import com.dms.dto.response.UserResponse;
import com.dms.entity.*;
import com.dms.exception.*;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for user-management operations.
 *
 * <h2>Deprecation instead of deletion</h2>
 * Users are <b>never hard-deleted</b>. The {@link #deprecateUser} method sets
 * the user's {@link DeprecationStatus} to {@link DeprecationStatus#DEPRECATED},
 * blocking login while preserving all data, document ownership, and audit
 * history. {@link #restoreUser} reverses the operation.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthServiceImpl authService;

    /**
     * Returns a paginated list of all <em>active</em> (non-deprecated) users.
     *
     * @param page zero-based page index
     * @param size page size
     * @param search optional search term; filters on name, e-mail, username
     * @return a {@link Page} of {@link UserResponse} DTOs
     */
    public Page<UserResponse> listUsers(int page, int size, String search) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.searchActiveUsers(search, pg)
                : userRepository.findByDeprecationStatus(DeprecationStatus.ACTIVE, pg);
        return users.map(authService::mapUserToResponse);
    }

    /**
     * Returns a paginated list of all <em>deprecated</em> users (admin view).
     *
     * @param page zero-based page index
     * @param size page size
     * @return a {@link Page} of {@link UserResponse} DTOs for deprecated users
     */
    public Page<UserResponse> listDeprecatedUsers(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("deprecatedAt").descending());
        return userRepository.findAllDeprecated(pg).map(authService::mapUserToResponse);
    }

    /**
     * Returns a single user's profile by ID.
     *
     * @param id the user ID
     * @return the {@link UserResponse} DTO
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    public UserResponse getUser(Long id) {
        return authService.mapUserToResponse(findById(id));
    }

    /**
     * Updates the authenticated user's own profile fields. Only non-null fields
     * are applied (partial update).
     *
     * @param email the e-mail of the user to update
     * @param req the fields to change
     * @return the updated {@link UserResponse}
     */
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        if (req.getPhoneNumber() != null) {
            user.setPhoneNumber(req.getPhoneNumber());
        }
        return authService.mapUserToResponse(userRepository.save(user));
    }

    /**
     * Replaces the complete set of roles assigned to a user.
     *
     * @param userId the ID of the user
     * @param roleNames the new full set of role name strings
     * @return the updated {@link UserResponse}
     * @throws ResourceNotFoundException if the user or any role name is invalid
     */
    @Transactional
    public UserResponse updateRoles(Long userId, List<String> roleNames) {
        User user = findById(userId);
        Set<Role> roles = roleNames.stream()
                .map(name -> {
                    try {
                        RoleName rn = RoleName.valueOf(name);
                        return roleRepository.findByName(rn)
                                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
                    } catch (IllegalArgumentException e) {
                        throw new ResourceNotFoundException("Invalid role: " + name);
                    }
                })
                .collect(Collectors.toSet());
        user.setRoles(roles);
        return authService.mapUserToResponse(userRepository.save(user));
    }

    /**
     * Activates a user account ({@code isActive = true}).
     *
     * @param id the user ID
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public void activateUser(Long id) {
        User user = findById(id);
        user.setIsActive(true);
        userRepository.save(user);
    }

    /**
     * Deactivates a user account ({@code isActive = false}) without deprecating
     * it. The user cannot log in but their record remains fully visible in
     * admin views.
     *
     * @param id the user ID
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public void deactivateUser(Long id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * <b>Deprecates</b> a user account — the soft, reversible alternative to
     * deletion.
     *
     * <p>
     * What happens:
     * <ul>
     * <li>{@link DeprecationStatus} is set to
     * {@link DeprecationStatus#DEPRECATED}.</li>
     * <li>{@code isActive} is set to {@code false} to block login
     * immediately.</li>
     * <li>{@code deprecatedAt}, {@code deprecationReason}, and
     * {@code deprecatedBy} are recorded for audit purposes.</li>
     * <li>All active refresh tokens are revoked to terminate existing
     * sessions.</li>
     * </ul>
     *
     * <p>
     * The user's documents, permissions, and all related data remain fully
     * intact. Use {@link #restoreUser} to reverse this operation.
     *
     * @param id the ID of the user to deprecate
     * @param reason human-readable reason for deprecation
     * @param deprecatedByUser the username of the admin performing the action
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalStateException if the user is already deprecated
     */
    @Transactional
    public UserResponse deprecateUser(Long id, String reason, String deprecatedByUser) {
        User user = findById(id);
        if (user.isDeprecated()) {
            throw new IllegalStateException("User is already deprecated");
        }
        user.setDeprecationStatus(DeprecationStatus.DEPRECATED);
        user.setIsActive(false);
        user.setDeprecatedAt(LocalDateTime.now());
        user.setDeprecationReason(reason);
        user.setDeprecatedBy(deprecatedByUser);
        refreshTokenRepository.revokeAllByUserId(id);
        return authService.mapUserToResponse(userRepository.save(user));
    }

    /**
     * <b>Restores</b> a previously deprecated user back to active status.
     *
     * <p>
     * What happens:
     * <ul>
     * <li>{@link DeprecationStatus} is reset to
     * {@link DeprecationStatus#ACTIVE}.</li>
     * <li>{@code isActive} is set to {@code true} so the user can log in
     * again.</li>
     * <li>All deprecation audit fields ({@code deprecatedAt},
     * {@code deprecationReason}, {@code deprecatedBy}) are cleared.</li>
     * </ul>
     *
     * @param id the ID of the deprecated user to restore
     * @return the updated {@link UserResponse}
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalStateException if the user is not currently deprecated
     */
    @Transactional
    public UserResponse restoreUser(Long id) {
        User user = findById(id);
        if (!user.isDeprecated()) {
            throw new IllegalStateException("User is not deprecated");
        }
        user.setDeprecationStatus(DeprecationStatus.ACTIVE);
        user.setIsActive(true);
        user.setDeprecatedAt(null);
        user.setDeprecationReason(null);
        user.setDeprecatedBy(null);
        return authService.mapUserToResponse(userRepository.save(user));
    }

    // ── Private helpers ───────────────────────────────────────────────────
    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
