package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Unified token table that consolidates RefreshToken, PasswordResetToken, and OtpToken
 * into a single {@code app_tokens} table, distinguished by {@link TokenType}.
 *
 * <p>The three legacy entity classes are kept for backward compatibility but now
 * delegate their persistence through this table via views / Spring Data projections.
 * New code should use this entity directly.
 *
 * <table>
 *   <tr><th>type</th><th>token_value used as</th></tr>
 *   <tr><td>REFRESH</td><td>JWT refresh token string</td></tr>
 *   <tr><td>PASSWORD_RESET</td><td>UUID reset link token</td></tr>
 *   <tr><td>OTP</td><td>6-digit numeric code</td></tr>
 * </table>
 */
@Entity
@Table(name = "app_tokens", indexes = {
    @Index(name = "idx_app_tokens_user",  columnList = "user_id"),
    @Index(name = "idx_app_tokens_value", columnList = "token_value"),
    @Index(name = "idx_app_tokens_type",  columnList = "token_type"),
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppToken {

    public enum TokenType {
        REFRESH,
        PASSWORD_RESET,
        OTP_LOGIN_2FA,
        OTP_ENABLE_2FA,
        OTP_SENSITIVE_ACTION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The raw token value — JWT string, UUID, or OTP code depending on type. */
    @Column(name = "token_value", nullable = false, length = 1024)
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 40)
    private TokenType tokenType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** For REFRESH tokens — whether explicitly revoked before expiry. */
    @Column(name = "is_revoked")
    @Builder.Default
    private Boolean isRevoked = false;

    /** For PASSWORD_RESET and OTP tokens — whether already consumed. */
    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;

    /** Number of failed verification attempts (OTP only). */
    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper predicates ─────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !Boolean.TRUE.equals(isUsed)
            && !Boolean.TRUE.equals(isRevoked)
            && !isExpired();
    }
}
