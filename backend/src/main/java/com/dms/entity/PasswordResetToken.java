package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity that stores a one-time password-reset token sent to a user by
 * email.
 *
 * <p>
 * Maps to the {@code password_reset_tokens} table. The flow is:
 * <ol>
 * <li>User requests a reset → a UUID token is generated and stored here.</li>
 * <li>An email is dispatched containing a link with the token as a query
 * parameter.</li>
 * <li>User clicks the link → the token is validated (not used, not
 * expired).</li>
 * <li>Password is updated → {@link #isUsed} is set to {@code true}.</li>
 * </ol>
 *
 * <p>
 * Old tokens for a user are deleted before a new one is created to prevent
 * token accumulation (see
 * {@link com.dms.service.impl.AuthServiceImpl#forgotPassword}).
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.service.impl.AuthServiceImpl
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The opaque UUID token embedded in the reset link. Must be unique across
     * all rows and treated as a secret.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user who requested the password reset.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The date/time after which this token is no longer valid. Typically
     * {@code now + 60 minutes} (configurable via
     * {@code app.password-reset.expiry-minutes}).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has already been consumed. A used token cannot be
     * reused even if it has not yet expired.
     */
    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;

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
