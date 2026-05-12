package com.dms.service.impl;

import com.dms.dto.request.UpdateProfileRequest;
import com.dms.dto.response.UserResponse;
import com.dms.entity.*;
import com.dms.exception.*;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for user management.
 *
 * <p>"Deprecation" of a user is implemented as {@code isActive = false}. A
 * deprecated user cannot log in (the security layer rejects them) but their
 * record is preserved for audit trail continuity. An admin can restore them
 * at any time with {@link #restore(Long, String)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthServiceImpl authService;
    private final AuditService    auditService;

    public Page<UserResponse> listUsers(int page, int size, String search) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = search != null && !search.isBlank()
                ? userRepository.searchUsers(search, pg)
                : userRepository.findAll(pg);
        return users.map(authService::mapUserToResponse);
    }

    /** Admin-only list of deprecated (inactive) users, for the "restore" UI. */
    public Page<UserResponse> listDeprecated(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return userRepository.findByIsActiveFalse(pg).map(authService::mapUserToResponse);
    }

    /**
     * Compact "employee directory" for UI filters (audit page, sharing
     * autocomplete). Returns minimal fields and does not paginate.
     */
    public List<Map<String, Object>> directory() {
        return userRepository.findAllForDirectory().stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        u.getId());
                    m.put("email",     u.getEmail());
                    m.put("firstName", u.getFirstName());
                    m.put("lastName",  u.getLastName());
                    m.put("isActive",  u.getIsActive());
                    m.put("roles",     u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    public UserResponse getUser(Long id) {
        return authService.mapUserToResponse(findById(id));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getFirstName()   != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()    != null) user.setLastName(req.getLastName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        return authService.mapUserToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateRoles(Long userId, List<String> roleNames, String actorEmail) {
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
        User saved = userRepository.save(user);
        auditService.log(actorEmail, null, AuditLog.Action.ROLE_CHANGE,
                "USER", userId,
                "Roles of " + user.getEmail() + " set to " + roleNames,
                null, null, 200);
        return authService.mapUserToResponse(saved);
    }

    // ── Deprecation (soft-lifecycle) ─────────────────────────────────────
    @Transactional
    public void deprecate(Long id, String reason, String actorEmail) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
        auditService.log(actorEmail, null, AuditLog.Action.USER_DEPRECATE,
                "USER", id, "Deprecated " + user.getEmail()
                        + (reason != null ? " — " + reason : ""),
                null, null, 200);
    }

    @Transactional
    public void restore(Long id, String actorEmail) {
        User user = findById(id);
        user.setIsActive(true);
        userRepository.save(user);
        auditService.log(actorEmail, null, AuditLog.Action.USER_RESTORE,
                "USER", id, "Restored " + user.getEmail(),
                null, null, 200);
    }

    // Back-compat: activate/deactivate == restore/deprecate
    @Transactional
    public void activateUser(Long id) {
        User user = findById(id);
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * Permanent hard delete. Only an admin should call this. In practice,
     * prefer {@link #deprecate(Long, String, String)} — hard deletion breaks
     * audit-trail references and cascades to the user's documents.
     */
    @Transactional
    public void deleteUser(Long id, String actorEmail) {
        User user = findById(id);
        userRepository.delete(user);
        auditService.log(actorEmail, null, AuditLog.Action.USER_DEACTIVATE,
                "USER", id, "Permanently deleted " + user.getEmail(),
                null, null, 200);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
