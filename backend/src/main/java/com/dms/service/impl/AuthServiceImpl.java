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

    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final AppTokenRepository      appTokenRepository;
    private final PasswordEncoder         passwordEncoder;
    private final JwtUtil                 jwtUtil;
    private final AuthenticationManager   authManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService            emailService;
    private final SettingsService         settingsService;
    private final UserApprovalRepository  approvalRepository;
    private final NotificationService     notificationService;

    @Value("${app.password-reset.expiry-minutes}")
    private int resetExpiryMinutes;

    // ── Login ─────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getTerminatedAt() != null) {
            throw new DisabledException(
                    "Your account has been terminated. Reason: "
                    + (user.getTerminationReason() == null ? "(not specified)" : user.getTerminationReason())
                    + ". Please contact your administrator.");
        }
        if (user.getResignationEffectiveDate() != null
                && !user.getResignationEffectiveDate().isAfter(LocalDateTime.now())) {
            throw new DisabledException(
                    "Your access has been revoked as your resignation took effect on "
                    + user.getResignationEffectiveDate().toLocalDate() + ".");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken  = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);

        AuthResponse resp = buildAuthResponse(accessToken, refreshToken, user);
        resp.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
        return resp;
    }

    // ── First-login forced password change ───────────────────────────
    @Transactional
    public void firstLoginPasswordChange(String userEmail,
            com.dms.dto.request.FirstLoginPasswordChangeRequest req) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        appTokenRepository.revokeAllRefreshTokensForUser(user);
    }

    // ── Register ──────────────────────────────────────────────────────
    @Transactional
    public Object register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            User existing = userRepository.findByEmail(request.getEmail()).orElseThrow();
            var prior = approvalRepository.findByUserId(existing.getId()).orElse(null);
            boolean wasRejected = prior != null
                    && prior.getStatus() == UserApproval.ApprovalStatus.REJECTED;
            if (wasRejected) {
                approvalRepository.delete(prior);
                userRepository.delete(existing);
                userRepository.flush();
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
        boolean autoApproved     = approvalRequired && matchesAutoApproveDomain(request.getEmail());
        boolean pending          = approvalRequired && !autoApproved;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .department(parseDepartmentForRegister(request.getDepartment()))
                .roles(new HashSet<>(Set.of(defaultRole)))
                .isActive(!pending)
                .isEmailVerified(false)
                .build();
        userRepository.save(user);

        if (pending) {
            approvalRepository.save(UserApproval.builder().user(user).build());
            notificationService.notifyAllAdmins(
                    Notification.NotificationType.PENDING_APPROVAL,
                    "New registration pending review",
                    user.getFirstName() + " " + user.getLastName()
                            + " (" + user.getEmail() + ") is awaiting approval.",
                    "/approvals"
            );
            log.info("Registration pending approval: {}", user.getEmail());
            return ApiResponse.ok("Registration received. An administrator will review your account.");
        }

        if (autoApproved) log.info("Registration auto-approved by domain rule: {}", user.getEmail());
        try { emailService.sendWelcomeEmail(user); }
        catch (Exception e) { log.warn("Welcome email failed: {}", e.getMessage()); }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken  = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    private boolean matchesAutoApproveDomain(String email) {
        String raw = settingsService.get("auto_approve_domains");
        if (raw == null || raw.isBlank() || email == null) return false;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return false;
        String emailDomain = email.substring(at + 1).toLowerCase().trim();
        for (String d : raw.split(",")) {
            String allowed = d.trim().toLowerCase();
            if (!allowed.isEmpty() && allowed.equals(emailDomain)) return true;
        }
        return false;
    }

    // ── Refresh token ─────────────────────────────────────────────────
    @Transactional
    public AuthResponse refreshToken(String tokenStr) {
        AppToken stored = appTokenRepository
                .findByTokenValueAndTokenType(tokenStr, AppToken.TokenType.REFRESH)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        if (!stored.isValid())
            throw new InvalidTokenException("Refresh token expired or revoked");
        stored.setIsRevoked(true);
        appTokenRepository.save(stored);
        UserDetails ud = userDetailsService.loadUserByUsername(stored.getUser().getEmail());
        return buildAuthResponse(
                jwtUtil.generateToken(ud),
                createRefreshToken(stored.getUser()),
                stored.getUser());
    }

    // ── Forgot / reset password ───────────────────────────────────────
    @Transactional
    public void forgotPassword(String email) {
        if (!domainCanReceiveMail(email)) {
            throw new com.dms.exception.BadRequestException(
                    "This email domain doesn't accept mail. Please double-check the address.");
        }
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("No account is registered with " + email + "."));

        // Delete any existing password-reset tokens for this user
        appTokenRepository.deleteByUserAndType(user, AppToken.TokenType.PASSWORD_RESET);

        String token = UUID.randomUUID().toString();
        appTokenRepository.save(AppToken.builder()
                .tokenValue(token)
                .tokenType(AppToken.TokenType.PASSWORD_RESET)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                .build());

        try { emailService.sendPasswordResetEmail(user, token); }
        catch (Exception e) { log.warn("Password reset email failed: {}", e.getMessage()); }
    }

    private boolean domainCanReceiveMail(String email) {
        if (email == null) return false;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return false;
        String domain = email.substring(at + 1).trim();
        if (domain.isEmpty() || domain.contains(" ")) return false;
        javax.naming.directory.DirContext ctx = null;
        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            ctx = new javax.naming.directory.InitialDirContext(env);
            try {
                javax.naming.directory.Attributes attrs =
                        ctx.getAttributes(domain, new String[]{"MX"});
                if (attrs.get("MX") != null && attrs.get("MX").size() > 0) return true;
            } catch (javax.naming.NameNotFoundException e) {
                log.info("Email domain rejected (NXDOMAIN): {}", domain); return false;
            } catch (javax.naming.NamingException ignored) {}
            try {
                javax.naming.directory.Attributes attrs =
                        ctx.getAttributes(domain, new String[]{"A", "AAAA"});
                boolean hasA    = attrs.get("A")    != null && attrs.get("A").size()    > 0;
                boolean hasAAAA = attrs.get("AAAA") != null && attrs.get("AAAA").size() > 0;
                if (hasA || hasAAAA) return true;
                log.info("Email domain rejected (no MX/A/AAAA): {}", domain); return false;
            } catch (javax.naming.NameNotFoundException e) {
                log.info("Email domain rejected (NXDOMAIN): {}", domain); return false;
            }
        } catch (Exception e) {
            log.warn("DNS infrastructure error checking {}: {}", domain, e.getMessage()); return true;
        } finally {
            if (ctx != null) try { ctx.close(); } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        AppToken token = appTokenRepository
                .findByTokenValueAndTokenType(request.getToken(), AppToken.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));
        if (!token.isValid())
            throw new InvalidTokenException("Reset token has expired or already been used");
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setIsUsed(true);
        appTokenRepository.save(token);
        appTokenRepository.revokeAllRefreshTokensForUser(user);
    }

    // ── Change password / logout ──────────────────────────────────────
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new BadCredentialsException("Current password is incorrect");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        appTokenRepository.revokeAllRefreshTokensForUser(user);
    }

    @Transactional
    public void logout(String userEmail) {
        userRepository.findByEmail(userEmail)
                .ifPresent(appTokenRepository::revokeAllRefreshTokensForUser);
    }

    // ── Internal helpers ──────────────────────────────────────────────
    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        appTokenRepository.save(AppToken.builder()
                .tokenValue(token)
                .tokenType(AppToken.TokenType.REFRESH)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
        return token;
    }

    private AuthResponse buildAuthResponse(String access, String refresh, User user) {
        return AuthResponse.builder()
                .accessToken(access).refreshToken(refresh)
                .tokenType("Bearer").user(mapUserToResponse(user)).build();
    }

    private com.dms.entity.Department parseDepartmentForRegister(String s) {
        if (s == null || s.isBlank()) return com.dms.entity.Department.OTHER;
        try { return com.dms.entity.Department.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return com.dms.entity.Department.OTHER; }
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
