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
 * <p>
 * "Deprecation" of a user is implemented as {@code isActive = false}. A
 * deprecated user cannot log in (the security layer rejects them) but their
 * record is preserved for audit trail continuity. An admin can restore them at
 * any time with {@link #restore(Long, String)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthServiceImpl authService;
    private final AuditService auditService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.dms.service.EmailService emailService;
    private final AppTokenRepository appTokenRepository;

    public Page<UserResponse> listUsers(int page, int size, String search) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = search != null && !search.isBlank()
                ? userRepository.searchUsers(search, pg)
                : userRepository.findAll(pg);
        return users.map(authService::mapUserToResponse);
    }

    /**
     * Admin-only list of deprecated (inactive) users, for the "restore" UI.
     */
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
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("firstName", u.getFirstName());
                    m.put("lastName", u.getLastName());
                    m.put("isActive", u.getIsActive());
                    m.put("roles", u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()));
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

    /**
     * Convert a department String (case-insensitive) to the enum, defaulting to
     * OTHER for null/empty/invalid values.
     */
    private com.dms.entity.Department parseDepartment(String s) {
        if (s == null || s.isBlank()) {
            return com.dms.entity.Department.OTHER;
        }
        try {
            return com.dms.entity.Department.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return com.dms.entity.Department.OTHER;
        }
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    // ── Admin creates user (random password emailed) ────────────────────────
    /**
     * Create a user account on behalf of someone. Generates a strong random
     * password, sets {@code mustChangePassword=true} so they're forced to
     * change it on first login, and emails them their temporary credentials.
     */
    @Transactional
    public UserResponse adminCreate(com.dms.dto.request.AdminCreateUserRequest req,
            String actorEmail) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new com.dms.exception.DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new com.dms.exception.DuplicateResourceException("Username already taken");
        }

        // Generate a strong temp password
        String tempPassword = generateTempPassword();

        // Resolve roles (default to EMPLOYEE)
        java.util.Set<Role> roles = new java.util.HashSet<>();
        java.util.List<String> roleNames = req.getRoles();
        if (roleNames == null || roleNames.isEmpty()) {
            roles.add(roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new ResourceNotFoundException("Default role not found")));
        } else {
            for (String n : roleNames) {
                try {
                    roles.add(roleRepository.findByName(RoleName.valueOf(n))
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)));
                } catch (IllegalArgumentException e) {
                    throw new ResourceNotFoundException("Invalid role: " + n);
                }
            }
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhoneNumber())
                .department(parseDepartment(req.getDepartment()))
                .roles(roles)
                .isActive(true)
                .isEmailVerified(false)
                .mustChangePassword(true)
                .build();
        userRepository.save(user);

        // Email the user their temp credentials (best-effort)
        try {
            emailService.sendAdminCreatedAccountEmail(user, tempPassword);
        } catch (Exception e) {
            /* logged inside email service */ }

        auditService.log(actorEmail, null, AuditLog.Action.USER_CREATE,
                "USER", user.getId(),
                "Admin-created account " + user.getEmail() + " with temporary password",
                null, null, 201);
        return authService.mapUserToResponse(user);
    }

    /**
     * Generates a 12-char password with mixed case, digits, and symbols.
     */
    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digit = "23456789";
        String symb = "!@#$%&*";
        String all = upper + lower + digit + symb;
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder b = new StringBuilder(12);
        // Guarantee at least one of each class
        b.append(upper.charAt(rnd.nextInt(upper.length())));
        b.append(lower.charAt(rnd.nextInt(lower.length())));
        b.append(digit.charAt(rnd.nextInt(digit.length())));
        b.append(symb.charAt(rnd.nextInt(symb.length())));
        for (int i = 4; i < 12; i++) {
            b.append(all.charAt(rnd.nextInt(all.length())));
        }
        // Shuffle (Fisher–Yates)
        char[] arr = b.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
        return new String(arr);
    }

    // ── Suspend (temporary block with session revocation) ────────────────────
    /**
     * Immediately suspend a user account. Differs from {@link #terminate} in that
     * it does NOT set the {@code terminatedAt} timestamp or send a termination
     * email — this is a reversible administrative suspension. Active sessions are
     * killed so the user is logged out instantly.
     */
    @Transactional
    public void suspend(Long id, String reason, String actorEmail) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to suspend a user");
        }
        User user = findById(id);
        if (user.getEmail().equals(actorEmail)) {
            throw new IllegalArgumentException("You cannot suspend your own account");
        }
        user.setIsActive(false);
        userRepository.save(user);

        // Kill all active sessions immediately so the user is force-logged-out
        try {
            appTokenRepository.revokeAllRefreshTokensByUserId(id);
        } catch (Exception ignored) {
        }

        auditService.log(actorEmail, null, AuditLog.Action.USER_SUSPEND,
                "USER", id,
                "Suspended " + user.getEmail() + " — " + reason,
                null, null, 200);
    }

    // ── Termination ─────────────────────────────────────────────────────────
    /**
     * Immediately revoke a user's access. Sets {@code isActive=false}, records
     * the reason and actor, revokes all refresh tokens (so any active session
     * cannot extend), writes an audit log entry, and emails the user.
     */
    @Transactional
    public void terminate(Long id, String reason, String actorEmail) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to terminate a user");
        }
        User user = findById(id);
        if (user.getEmail().equals(actorEmail)) {
            throw new IllegalArgumentException("You cannot terminate your own account");
        }
        user.setIsActive(false);
        user.setTerminatedAt(java.time.LocalDateTime.now());
        user.setTerminationReason(reason);
        user.setTerminatedBy(actorEmail);
        userRepository.save(user);

        // Kill all active sessions immediately
        try {
            appTokenRepository.revokeAllRefreshTokensByUserId(id);
        } catch (Exception ignored) {
        }

        auditService.log(actorEmail, null, AuditLog.Action.USER_TERMINATE,
                "USER", id,
                "Terminated " + user.getEmail() + " — " + reason,
                null, null, 200);

        try {
            emailService.sendTerminationEmail(user, reason, actorEmail);
        } catch (Exception ignored) {
        }
    }

    // ── Resignation ─────────────────────────────────────────────────────────
    /**
     * Record a resignation. The user keeps access until the effective date
     * (default: last day of the current month at 23:59). A scheduled job checks
     * this field daily and revokes access when the date passes.
     *
     * @param id id of the resigning user (or null if employee self-resigns)
     * @param req reason and optional admin-set effective date
     * @param actorEmail email of the user performing the action
     */
    @Transactional
    public void resign(Long id, com.dms.dto.request.ResignationRequest req, String actorEmail) {
        User user = findById(id);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime effective;

        if (req.getEffectiveDate() != null) {
            effective = req.getEffectiveDate().atTime(23, 59, 59);
        } else {
            // Default to last day of current month, 23:59:59
            java.time.LocalDate today = now.toLocalDate();
            java.time.LocalDate lastOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            effective = lastOfMonth.atTime(23, 59, 59);
        }

        user.setResignationDate(now);
        user.setResignationEffectiveDate(effective);
        userRepository.save(user);

        auditService.log(actorEmail, null, AuditLog.Action.USER_RESIGN,
                "USER", id,
                "Resignation recorded for " + user.getEmail()
                + " — effective " + effective.toLocalDate()
                + (req.getReason() != null ? " — " + req.getReason() : ""),
                null, null, 200);

        try {
            emailService.sendResignationConfirmedEmail(user, effective);
        } catch (Exception ignored) {
        }
    }
}
