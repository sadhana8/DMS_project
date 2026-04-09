package com.dms.config;

import com.dms.entity.Role;
import com.dms.entity.RoleName;
import com.dms.entity.User;
import com.dms.repository.RoleRepository;
import com.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

/**
 * Seeds mandatory reference data into the database on every application
 * startup.
 *
 * <p>
 * Implements {@link CommandLineRunner} so it executes automatically once the
 * Spring context is fully initialised, but <em>before</em> the embedded Tomcat
 * begins accepting HTTP requests.
 *
 * <h2>What is seeded</h2>
 * <ol>
 * <li><b>Roles</b> – one row for each value in {@link RoleName}
 *       ({@code ROLE_ADMIN}, {@code ROLE_MANAGER}, {@code ROLE_EDITOR},
 * {@code ROLE_VIEWER}). Existing rows are left untouched (idempotent).</li>
 * <li><b>Default admin user</b> – {@code admin@dms.com} with password
 * {@code Admin@123}. Created only when no user with that e-mail exists.
 * <b>Change this password immediately in any non-local environment.</b></li>
 * </ol>
 *
 * <p>
 * Both operations are wrapped in a single transaction so a partial failure
 * leaves the database in a consistent state.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see RoleName
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    /**
     * Repository used to read and persist {@link Role} entities.
     */
    private final RoleRepository roleRepository;

    /**
     * Repository used to read and persist {@link User} entities.
     */
    private final UserRepository userRepository;

    /**
     * BCrypt encoder used to hash the default admin password at startup.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Executes the seed logic once the Spring context is ready.
     *
     * <p>
     * Calls {@link #seedRoles()} first because the admin user depends on the
     * {@code ROLE_ADMIN} and {@code ROLE_MANAGER} rows already existing.
     *
     * @param args command-line arguments (not used)
     */
    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
    }

    /**
     * Ensures every {@link RoleName} constant has a corresponding row in the
     * {@code roles} table.
     *
     * <p>
     * Uses a simple existence check so re-running the application never creates
     * duplicate roles.
     */
    private void seedRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
                log.info("Created role: {}", roleName);
            }
        });
    }

    /**
     * Creates the default system administrator account if it does not exist
     * yet.
     *
     * <p>
     * The admin is assigned both {@code ROLE_ADMIN} and {@code ROLE_MANAGER} so
     * they have full document-management capabilities in addition to
     * user-administration rights.
     *
     * <p>
     * <b>Security note:</b> the password {@code Admin@123} is intentionally
     * weak and must be changed before the application is exposed to any
     * network.
     */
    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@dms.com").isPresent()) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        Role managerRole = roleRepository.findByName(RoleName.ROLE_MANAGER).orElseThrow();

        User admin = User.builder()
                .username("admin")
                .email("admin@dms.com")
                .password(passwordEncoder.encode("Admin@123"))
                .firstName("System")
                .lastName("Administrator")
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(adminRole, managerRole))
                .build();
        userRepository.save(admin);
        log.info("Default admin created — email: admin@dms.com  password: Admin@123");
        log.warn("CHANGE THE DEFAULT ADMIN PASSWORD IMMEDIATELY IN PRODUCTION!");
    }
}
