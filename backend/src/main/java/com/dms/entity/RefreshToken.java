package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity that stores an active refresh token issued to a user.
 *
 * <p>
 * Maps to the {@code refresh_tokens} table. Unlike access tokens (which are
 * verified purely by signature), refresh tokens are persisted so they can be
 * explicitly revoked — for example, when the user logs out, changes their
 * password, or an admin deactivates the account.
 *
 * <h2>Rotation strategy</h2>
 * Each call to the {@code /auth/refresh-token} endpoint:
 * <ol>
 * <li>Marks the incoming token as revoked
 * ({@link #isRevoked} = {@code true}).</li>
 * <li>Issues a brand-new refresh token and persists it.</li>
 * </ol>
 * This one-time-use rotation limits the window of exploitation if a token is
 * stolen.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.service.impl.AuthServiceImpl
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The opaque UUID token sent to and stored by the client. Must be unique
     * across all rows.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user this refresh token was issued to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The absolute date/time after which this token is no longer valid.
     * Typically {@code now + 7 days}.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Soft-revocation flag. Set to {@code true} when the token is consumed, the
     * user logs out, or their password is changed.
     */
    @Column(name = "is_revoked")
    @Builder.Default
    private Boolean isRevoked = false;

    /**
     * Timestamp when this token was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Convenience method to check whether the token has passed its expiry time.
     *
     * @return {@code true} if the current time is after {@link #expiresAt}
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
