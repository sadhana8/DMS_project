package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a security role in the DocVault system.
 *
 * <p>
 * Maps to the {@code roles} table. Roles are seeded once at startup by
 * {@link com.dms.config.DataInitializer} and are not expected to be created or
 * deleted by end-users at runtime.
 *
 * <p>
 * The {@link #name} field is stored as a string in the database (e.g.
 * {@code "ROLE_ADMIN"}) so that database queries remain readable without
 * needing to join an ordinal lookup.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see RoleName
 * @see User
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The canonical role name stored as its enum string representation. Must be
     * unique across all rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    /**
     * Convenience constructor used by {@link com.dms.config.DataInitializer} to
     * create a role with only a name (ID is auto-assigned).
     *
     * @param name the {@link RoleName} for this role
     */
    public Role(RoleName name) {
        this.name = name;
    }
}
