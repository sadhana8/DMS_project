package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a registered user of the DocVault system.
 *
 * <p>
 * Maps to the {@code users} table. E-mail and username are both unique
 * identifiers; e-mail is used as the Spring Security principal name.
 *
 * <h2>Soft deprecation — no hard deletes</h2>
 * Users are <b>never physically deleted</b>. Calling the deprecate endpoint
 * sets {@link #deprecationStatus} to {@link DeprecationStatus#DEPRECATED},
 * records {@link #deprecatedAt}, {@link #deprecationReason}, and
 * {@link #deprecatedBy}. The account can be fully restored by an admin. This
 * preserves document ownership references and full audit history.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see DeprecationStatus
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "email"),
            @UniqueConstraint(columnNames = "username")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_picture")
    private String profilePicture;

    /**
     * Account enabled flag — distinct from deprecation.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    /**
     * Soft-deprecation lifecycle state. Defaults to
     * {@link DeprecationStatus#ACTIVE}. Never set this to
     * {@link DeprecationStatus#PERMANENTLY_DELETED} except through the admin
     * endpoint after the retention period.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deprecation_status", nullable = false)
    @Builder.Default
    private DeprecationStatus deprecationStatus = DeprecationStatus.ACTIVE;

    /**
     * Timestamp of deprecation; {@code null} when active.
     */
    @Column(name = "deprecated_at")
    private LocalDateTime deprecatedAt;

    /**
     * Human-readable reason provided by the admin who deprecated this account.
     */
    @Column(name = "deprecation_reason", columnDefinition = "TEXT")
    private String deprecationReason;

    /**
     * Username of the admin who performed the deprecation. Stored as a plain
     * string (not a FK) to preserve the audit trail even if the admin's own
     * account is later deprecated.
     */
    @Column(name = "deprecated_by")
    private String deprecatedBy;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Returns {@code true} if this user has been deprecated or permanently
     * deleted.
     *
     * @return {@code true} when not in {@link DeprecationStatus#ACTIVE} state
     */
    public boolean isDeprecated() {
        return deprecationStatus != DeprecationStatus.ACTIVE;
    }
}
