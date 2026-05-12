package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores per-user 2FA preference. Kept in a separate table so the existing
 * {@code users} table schema is untouched. One row per user; absence of a
 * row means 2FA is disabled.
 */
@Entity
@Table(name = "user_two_factor_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
