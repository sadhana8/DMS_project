package com.dms.repository;

import com.dms.entity.Role;
import com.dms.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Role} entities.
 *
 * <p>
 * Roles are seeded once at startup and are rarely queried directly; the primary
 * use-case is looking up a role by its {@link RoleName} so it can be assigned
 * to a user.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a role by its enum name.
     *
     * @param name the {@link RoleName} constant to look up
     * @return an {@link Optional} containing the role, or empty if it has not
     * been seeded yet
     */
    Optional<Role> findByName(RoleName name);
}
