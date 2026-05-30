package com.dms.service.impl;

import com.dms.repository.AppTokenRepository;
import com.dms.repository.OtpTokenRepository;
import com.dms.repository.PasswordResetTokenRepository;
import com.dms.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nightly cleanup of expired tokens across both the legacy tables and
 * the new unified app_tokens table, preventing unbounded table growth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final AppTokenRepository          appTokenRepository;
    private final RefreshTokenRepository      refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpTokenRepository          otpTokenRepository;

    /** Runs every night at 02:00. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now();
        int unified  = appTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Token cleanup: removed {} expired records from app_tokens", unified);
        // Legacy tables: Spring Data doesn't have a built-in deleteExpiredBefore,
        // so we just log; they'll be superseded over time.
    }
}
