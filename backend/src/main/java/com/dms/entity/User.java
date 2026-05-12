package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * Postal address — free-text, up to 500 chars.
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * Department the employee belongs to. Defaults to OTHER for existing users
     * (NULL in DB) — see UserResponse mapping. New users created via
     * admin-create or registration must specify it.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "department", length = 20)
    private Department department;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

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
     * Forces a password change on next login (used for admin-created accounts).
     */
    @Column(name = "must_change_password")
    @Builder.Default
    private Boolean mustChangePassword = false;

    /**
     * Date a user resigned. Their access ends on resignationEffectiveDate.
     */
    @Column(name = "resignation_date")
    private LocalDateTime resignationDate;

    /**
     * When the resignation takes effect (typically end of resignation month).
     */
    @Column(name = "resignation_effective_date")
    private LocalDateTime resignationEffectiveDate;

    /**
     * When the user was terminated (immediate). Null if not terminated.
     */
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    /**
     * Reason given by the admin who performed the termination.
     */
    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    /**
     * Email of the admin who performed the termination.
     */
    @Column(name = "terminated_by")
    private String terminatedBy;
}
