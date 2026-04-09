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

/**
 * Service implementation covering all authentication and credential-management
 * operations for the DocVault application.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 * <li><b>Login</b> – validates credentials, updates last-login, issues access
 * and refresh tokens.</li>
 * <li><b>Register</b> – creates a new user with the default VIEWER role, sends
 * a welcome e-mail.</li>
 * <li><b>Token refresh</b> – validates, revokes, and rotates refresh
 * tokens.</li>
 * <li><b>Forgot password</b> – generates a one-time token and emails a reset
 * link.</li>
 * <li><b>Reset password</b> – validates the token, updates the password, marks
 * the token as used, revokes all refresh tokens.</li>
 * <li><b>Change password</b> – verifies the current password before
 * updating.</li>
 * <li><b>Logout</b> – revokes all refresh tokens for the user.</li>
 * <li><b>User mapping</b> – converts {@link User} entities to
 * {@link UserResponse} DTOs; shared by other services.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see JwtUtil
 * @see EmailService
 */
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

    /**
     * Password-reset token expiry window in minutes. Injected from
     * {@code app.password-reset.expiry-minutes}.
     */
    @Value("${app.password-reset.expiry-minutes}")
    private int resetExpiryMinutes;

    // ── Login ─────────────────────────────────────────────────────────────
    /**
     * Authenticates a user with their e-mail and password, then issues JWT
     * access and refresh tokens.
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Delegates credential validation to
     * {@link AuthenticationManager}.</li>
     * <li>Updates {@link User#getLastLogin()} to {@code now}.</li>
     * <li>Generates a short-lived access token and a long-lived refresh
     * token.</li>
     * </ol>
     *
     * @param request the {@link LoginRequest} containing e-mail and password
     * @return an {@link AuthResponse} with both tokens and the user's profile
     * data
     * @throws BadCredentialsException if the credentials are invalid
     * @throws DisabledException if the account has been deactivated
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // ── Register ──────────────────────────────────────────────────────────
    /**
     * Creates a new user account, assigns the default {@code ROLE_VIEWER} role,
     * sends a welcome e-mail, and immediately issues authentication tokens so
     * the user is logged in after registration.
     *
     * @param request the {@link RegisterRequest} with username, e-mail,
     * password, and name fields
     * @return an {@link AuthResponse} with tokens and the new user's profile
     * data
     * @throws DuplicateResourceException if the e-mail or username is already
     * registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }

        Role viewerRole = roleRepository.findByName(RoleName.ROLE_VIEWER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .roles(new HashSet<>(Set.of(viewerRole)))
                .build();
        userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user);
        } catch (Exception e) {
            log.warn("Welcome email failed for {}: {}", user.getEmail(), e.getMessage());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // ── Token refresh ─────────────────────────────────────────────────────
    /**
     * Validates an existing refresh token, revokes it, and issues a brand-new
     * access/refresh token pair (rotation strategy).
     *
     * @param refreshTokenStr the opaque refresh-token string from the client
     * @return a new {@link AuthResponse} with fresh tokens
     * @throws InvalidTokenException if the token is not found, revoked, or
     * expired
     */
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

    // ── Forgot / reset password ───────────────────────────────────────────
    /**
     * Initiates the password-reset flow by generating a one-time token and
     * dispatching a reset-link e-mail.
     *
     * <p>
     * If no user with the given e-mail exists, the method returns silently
     * without throwing an exception. This prevents user-enumeration attacks
     * (the client sees the same response regardless of whether the account
     * exists).
     *
     * @param email the e-mail address submitted via the "forgot password" form
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                .build();
        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user, token);
    }

    /**
     * Completes the password-reset flow by validating the token and updating
     * the user's password.
     *
     * <p>
     * After a successful reset:
     * <ul>
     * <li>The reset token is marked as used.</li>
     * <li>All existing refresh tokens are revoked to force re-login on all
     * devices.</li>
     * </ul>
     *
     * @param request the {@link ResetPasswordRequest} containing the token and
     * new password
     * @throws InvalidTokenException if the token is not found, already used, or
     * expired
     */
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

    // ── Change password ───────────────────────────────────────────────────
    /**
     * Allows an authenticated user to change their own password after verifying
     * the current one.
     *
     * <p>
     * All existing refresh tokens are revoked after a successful change so any
     * other active sessions are terminated.
     *
     * @param userEmail the e-mail of the currently authenticated user
     * @param request the {@link ChangePasswordRequest} with current and new
     * passwords
     * @throws BadCredentialsException if {@code currentPassword} does not match
     * the stored hash
     * @throws ResourceNotFoundException if the user is not found (should not
     * normally occur)
     */
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

    // ── Logout ────────────────────────────────────────────────────────────
    /**
     * Logs the user out by revoking all of their active refresh tokens. The
     * short-lived access token will naturally expire; the client is responsible
     * for discarding it from local storage.
     *
     * @param userEmail the e-mail of the currently authenticated user
     */
    @Transactional
    public void logout(String userEmail) {
        userRepository.findByEmail(userEmail)
                .ifPresent(u -> refreshTokenRepository.revokeAllByUserId(u.getId()));
    }

    // ── Shared mapper ─────────────────────────────────────────────────────
    /**
     * Converts a {@link User} JPA entity to a {@link UserResponse} DTO safe for
     * inclusion in API responses (no password hash, etc.).
     *
     * <p>
     * This method is package-accessible so that {@link DocumentServiceImpl} and
     * {@link UserServiceImpl} can reuse it without duplicating mapping code.
     *
     * @param user the entity to convert; must not be {@code null}
     * @return a fully-populated {@link UserResponse}
     */
    public UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────
    /**
     * Persists a new {@link RefreshToken} for the given user and returns its
     * opaque UUID string.
     *
     * @param user the owner of the new token
     * @return the raw token string to send to the client
     */
    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
        return token;
    }

    /**
     * Assembles an {@link AuthResponse} from the raw token strings and user
     * entity.
     *
     * @param access the JWT access-token string
     * @param refresh the opaque refresh-token string
     * @param user the authenticated user entity
     * @return the fully-populated {@link AuthResponse}
     */
    private AuthResponse buildAuthResponse(String access, String refresh, User user) {
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .user(mapUserToResponse(user))
                .build();
    }
}
