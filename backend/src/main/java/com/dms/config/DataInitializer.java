package com.dms.config;

import com.dms.entity.*;
import com.dms.repository.*;
import com.dms.service.impl.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SettingsService settingsService;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        // Must run BEFORE seedRoles() and OUTSIDE any JPA transaction
        dropRolesNameCheckConstraint();
        seedRoles();
        seedAdmin();
        settingsService.ensureDefaults();
        log.info("Data initialization complete");
    }

    /**
     * Drops the auto-generated Hibernate 6 CHECK constraint on roles.name so
     * that newly added role values can be inserted. Uses a raw JDBC connection
     * with auto-commit so the DDL is committed immediately, independent of any
     * JPA transaction.
     */
    private void dropRolesNameCheckConstraint() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(true);
            stmt.execute("ALTER TABLE IF EXISTS roles DROP CONSTRAINT IF EXISTS roles_name_check");
            log.info("roles_name_check constraint removed (or was already absent)");
        } catch (Exception e) {
            log.warn("Could not drop roles_name_check constraint: {}", e.getMessage());
        }
    }

    @Transactional
    public void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (roleRepo.findByName(name).isEmpty()) {
                roleRepo.save(Role.builder().name(name).build());
                log.info("Seeded role: {}", name);
            }
        }
    }

    @Transactional
    public void seedAdmin() {
        if (userRepo.findByEmail("admin@dms.com").isEmpty()) {
            Role adminRole = roleRepo.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role hrRole = roleRepo.findByName(RoleName.ROLE_HR).orElseThrow();
            User admin = User.builder()
                    .username("admin")
                    .email("admin@dms.com")
                    .password(encoder.encode("Admin@123"))
                    .firstName("System").lastName("Admin")
                    .isActive(true).isEmailVerified(true)
                    .roles(new HashSet<>(Set.of(adminRole, hrRole)))
                    .build();
            userRepo.save(admin);
            log.info("Default admin created: admin@dms.com / Admin@123");
        }
    }
}
