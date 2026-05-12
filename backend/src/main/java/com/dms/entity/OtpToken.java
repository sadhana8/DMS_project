package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One-time password token. Stores a 6-digit numeric code bound to a user
 * and a {@link Purpose}. Verifying a token marks it used; expired or used
 * tokens are rejected. Backs the optional 2FA flow exposed by
 * {@code TwoFactorController}.
 *
 * <p>This is an additive feature; it does not modify the primary
 * password-based login implemented by {@code AuthServiceImpl}.
 */
@Entity
@Table(name = "otp_tokens",
        indexes = {
            @Index(name = "idx_otp_user", columnList = "user_id"),
            @Index(name = "idx_otp_code", columnList = "code")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    public enum Purpose {
        LOGIN_2FA,
        ENABLE_2FA,
        SENSITIVE_ACTION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !Boolean.TRUE.equals(isUsed) && !isExpired();
    }
}
