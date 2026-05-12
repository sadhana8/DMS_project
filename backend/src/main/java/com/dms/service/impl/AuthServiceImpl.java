package com.dms.service.impl;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.entity.*;
import com.dms.exception.*;
import com.dms.repository.*;
import com.dms.security.CustomUserDetailsService;
import com.dms.service.EmailService;
import com.dms.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final SettingsService settingsService;
    private final UserApprovalRepository approvalRepository;
    private final NotificationService notificationService;

    @Value("${app.password-reset.expiry-minutes}")
    private int resetExpiryMinutes;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Terminated users can never log in
        if (user.getTerminatedAt() != null) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Your account has been terminated. Reason: "
                    + (user.getTerminationReason() == null ? "(not specified)" : user.getTerminationReason())
                    + ". Please contact your administrator.");
        }
        // Resigned users lose access at effective date
        if (user.getResignationEffectiveDate() != null
                && !user.getResignationEffectiveDate().isAfter(LocalDateTime.now())) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Your access has been revoked as your resignation took effect on "
                    + user.getResignationEffectiveDate().toLocalDate() + ".");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);

        AuthResponse resp = buildAuthResponse(accessToken, refreshToken, user);
        resp.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
        return resp;
    }

    /**
     * Used by admin-created accounts on first login. The user has just
     * authenticated via a temp password; this method changes it without
     * requiring the current one again, and clears the
     * {@code mustChangePassword} flag.
     */
    @Transactional
    public void firstLoginPasswordChange(String userEmail,
            com.dms.dto.request.FirstLoginPasswordChangeRequest req) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Register a new user.
     *
     * <p>
     * Flow branches on three settings:
     * <ol>
     * <li><b>Re-application after rejection.</b> If the email already exists
     * but the user has a REJECTED approval entry, we wipe that record and
     * create a fresh pending registration. Any other duplicate email is
     * rejected.</li>
     * <li><b>Auto-approval by domain.</b> If {@code require_admin_approval} is
     * on but the email's domain matches an entry in
     * {@code auto_approve_domains} (comma- separated list, e.g.
     * "acme.com,partner.com"), the user is approved immediately and gets
     * tokens.</li>
     * <li><b>Otherwise pending.</b> User is saved inactive, a UserApproval row
     * is created, and an in-app notification is fanned out to every active
     * admin.</li>
     * </ol>
     *
     * <p>
     * Returns {@link AuthResponse} when the user can log in immediately, or
     * {@link ApiResponse} when the user is pending admin approval.
     */
    @Transactional
    public Object register(RegisterRequest request) {
        // Step 1: Check for re-application after rejection
        if (userRepository.existsByEmail(request.getEmail())) {
            User existing = userRepository.findByEmail(request.getEmail()).orElseThrow();
            var prior = approvalRepository.findByUserId(existing.getId()).orElse(null);
            boolean wasRejected = prior != null
                    && prior.getStatus() == UserApproval.ApprovalStatus.REJECTED;

            if (wasRejected) {
                // Allow re-application: delete the old approval + user records.
                approvalRepository.delete(prior);
                userRepository.delete(existing);
                userRepository.flush();   // ensure delete is applied before insert
                log.info("Re-application after rejection: {}", request.getEmail());
            } else {
                throw new DuplicateResourceException("Email already registered");
            }
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        boolean approvalRequired = settingsService.getBool("require_admin_approval");
        boolean autoApproved = approvalRequired && matchesAutoApproveDomain(request.getEmail());
        boolean pending = approvalRequired && !autoApproved;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .department(parseDepartmentForRegister(request.getDepartment()))
                .roles(new HashSet<>(Set.of(defaultRole)))
                .isActive(!pending) // active unless we're parking for review
                .isEmailVerified(false)
                .build();
        userRepository.save(user);

        if (pending) {
            approvalRepository.save(UserApproval.builder().user(user).build());
            // Fan-out notification to all active admins so they see the badge update
            notificationService.notifyAllAdmins(
                    Notification.NotificationType.PENDING_APPROVAL,
                    "New registration pending review",
                    user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ") is awaiting approval.",
                    "/approvals"
            );
            log.info("Registration pending approval: {}", user.getEmail());
            return ApiResponse.ok("Registration received. An administrator will review your account.");
        }

        // Either approval was off, or domain auto-approved → log them in immediately.
        if (autoApproved) {
            log.info("Registration auto-approved by domain rule: {}", user.getEmail());
        }
        try {
            emailService.sendWelcomeEmail(user);
        } catch (Exception e) {
            log.warn("Welcome email failed: {}", e.getMessage());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    /**
     * Returns true if the email's domain (case-insensitive) appears in the
     * comma-separated {@code auto_approve_domains} setting. Empty or missing
     * setting always returns false.
     */
    private boolean matchesAutoApproveDomain(String email) {
        String raw = settingsService.get("auto_approve_domains");
        if (raw == null || raw.isBlank() || email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String emailDomain = email.substring(at + 1).toLowerCase().trim();
        for (String d : raw.split(",")) {
            String allowed = d.trim().toLowerCase();
            if (!allowed.isEmpty() && allowed.equals(emailDomain)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        if (stored.getIsRevoked() || stored.isExpired()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        stored.setIsRevoked(true);
        refreshTokenRepository.save(stored);

        UserDetails userDetails = userDetailsService.loadUserByUsername(stored.getUser().getEmail());
        String newAccess = jwtUtil.generateToken(userDetails);
        String newRefresh = createRefreshToken(stored.getUser());
        return buildAuthResponse(newAccess, newRefresh, stored.getUser());
    }

    @Transactional
    public void forgotPassword(String email) {
        // Step 1: Format check is already done at the controller via @Email/@Pattern.
        //         Here we only need domain validity + user existence.

        // Step 2: Check if the email domain has a mail server (MX record).
        //         This catches addresses with invalid/non-existent domains
        //         like @gds.com, @fakedomain.xyz, etc.
        if (!domainCanReceiveMail(email)) {
            throw new com.dms.exception.BadRequestException(
                    "This email domain doesn't accept mail. Please double-check the address.");
        }

        // Step 3: Reject if no user exists with this email.
        //         This is the explicit-failure mode the user requested.
        //         (Default best-practice would silently succeed to prevent
        //         account enumeration; we trade that off for clearer UX.)
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException(
                        "No account is registered with " + email + "."));

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token).user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                .build();
        passwordResetTokenRepository.save(resetToken);
        try {
            emailService.sendPasswordResetEmail(user, token);
        } catch (Exception e) {
            log.warn("Password reset email failed: {}", e.getMessage());
        }
    }

    /**
     * Checks whether the email's domain has a mail server (MX record) — or, as
     * a fallback, any address record (A/AAAA). Returns false for typos and
     * non-existent domains like {@code gds.com} or {@code fakemail.xyz}.
     *
     * <p>
     * Implementation uses JNDI's built-in DNS provider — no extra dependency. A
     * short timeout is set so a slow DNS doesn't hold up the request.
     */
    private boolean domainCanReceiveMail(String email) {
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).trim();
        if (domain.isEmpty() || domain.contains(" ")) {
            return false;
        }

        javax.naming.directory.DirContext ctx = null;
        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            ctx = new javax.naming.directory.InitialDirContext(env);

            // Try MX first — what real mail servers publish
            try {
                javax.naming.directory.Attributes attrs
                        = ctx.getAttributes(domain, new String[]{"MX"});
                if (attrs.get("MX") != null && attrs.get("MX").size() > 0) {
                    return true;
                }
            } catch (javax.naming.NameNotFoundException e) {
                // The DNS server explicitly said this domain doesn't exist.
                log.info("Email domain rejected (NXDOMAIN): {}", domain);
                return false;
            } catch (javax.naming.NamingException ignored) {
                // Could be "no MX record but the domain exists" — fall through to A check.
            }

            // Fall back to any address record. Many small domains don't publish MX
            // but accept mail anyway via their A record.
            try {
                javax.naming.directory.Attributes attrs
                        = ctx.getAttributes(domain, new String[]{"A", "AAAA"});
                boolean hasA = attrs.get("A") != null && attrs.get("A").size() > 0;
                boolean hasAAAA = attrs.get("AAAA") != null && attrs.get("AAAA").size() > 0;
                if (hasA || hasAAAA) {
                    return true;
                }
                log.info("Email domain rejected (no MX/A/AAAA): {}", domain);
                return false;
            } catch (javax.naming.NameNotFoundException e) {
                log.info("Email domain rejected (NXDOMAIN): {}", domain);
                return false;
            }
        } catch (Exception e) {
            // Only here if DNS itself failed (e.g., no network). Allow rather
            // than block everyone in an outage.
            log.warn("DNS infrastructure error checking {}: {}", domain, e.getMessage());
            return true;
        } finally {
            if (ctx != null) try {
                ctx.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));
        if (resetToken.getIsUsed() || resetToken.isExpired()) {
            throw new InvalidTokenException("Reset token has expired or already been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public void logout(String userEmail) {
        userRepository.findByEmail(userEmail)
                .ifPresent(u -> refreshTokenRepository.revokeAllByUserId(u.getId()));
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .token(token).user(user)
                .expiresAt(LocalDateTime.now().plusDays(7)).build();
        refreshTokenRepository.save(rt);
        return token;
    }

    private AuthResponse buildAuthResponse(String access, String refresh, User user) {
        return AuthResponse.builder()
                .accessToken(access).refreshToken(refresh)
                .tokenType("Bearer").user(mapUserToResponse(user)).build();
    }

    /**
     * Parse a department string from the register request, defaulting to OTHER.
     */
    private com.dms.entity.Department parseDepartmentForRegister(String s) {
        if (s == null || s.isBlank()) {
            return com.dms.entity.Department.OTHER;
        }
        try {
            return com.dms.entity.Department.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return com.dms.entity.Department.OTHER;
        }
    }

    public UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .department(user.getDepartment() != null
                        ? user.getDepartment().name()
                        : com.dms.entity.Department.OTHER.name())
                .profilePicture(user.getProfilePicture())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .mustChangePassword(user.getMustChangePassword())
                .resignationDate(user.getResignationDate())
                .resignationEffectiveDate(user.getResignationEffectiveDate())
                .terminatedAt(user.getTerminatedAt())
                .terminationReason(user.getTerminationReason())
                .terminatedBy(user.getTerminatedBy())
                .build();
    }
}
