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
import java.util.*;

@Component @RequiredArgsConstructor @Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository    roleRepo;
    private final UserRepository    userRepo;
    private final PasswordEncoder   encoder;
    private final SettingsService   settingsService;

    @Override @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        settingsService.ensureDefaults();
        log.info("Data initialization complete");
    }

    private void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (roleRepo.findByName(name).isEmpty()) {
                roleRepo.save(Role.builder().name(name).build());
                log.info("Seeded role: {}", name);
            }
        }
    }

    private void seedAdmin() {
        if (userRepo.findByEmail("admin@dms.com").isEmpty()) {
            Role adminRole = roleRepo.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role hrRole    = roleRepo.findByName(RoleName.ROLE_HR).orElseThrow();
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
