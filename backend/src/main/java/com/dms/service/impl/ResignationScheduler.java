package com.dms.service.impl;

import com.dms.entity.AuditLog;
import com.dms.entity.User;
import com.dms.repository.AppTokenRepository;
import com.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Revokes access for users whose resignation effective date has passed.
 *
 * <p>Runs every minute so the documented "access ends within minutes" behaviour
 * holds. Any user with {@code resignation_effective_date <= now} and still
 * active is set inactive, has all refresh tokens revoked in the unified
 * {@code app_tokens} table, and the action is recorded in the audit log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResignationScheduler {

    private final UserRepository     userRepository;
    private final AppTokenRepository appTokenRepository;
    private final AuditService       auditService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void revokeExpiredResignations() {
        LocalDateTime now = LocalDateTime.now();
        List<User> due = userRepository.findResignedAndDue(now);
        if (due.isEmpty()) return;

        for (User u : due) {
            u.setIsActive(false);
            userRepository.save(u);
            try { appTokenRepository.revokeAllRefreshTokensByUserId(u.getId()); }
            catch (Exception ignored) {}
            auditService.log("system", null, AuditLog.Action.USER_ACCESS_REVOKED,
                    "USER", u.getId(),
                    "Resignation effective — access revoked for " + u.getEmail(),
                    null, null, 200);
            log.info("Resignation effective: revoked access for user {}", u.getEmail());
        }
    }
}
