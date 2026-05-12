package com.dms.service.impl;

import com.dms.entity.OtpToken;
import com.dms.entity.TwoFactorSetting;
import com.dms.entity.User;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.OtpTokenRepository;
import com.dms.repository.TwoFactorSettingRepository;
import com.dms.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Generates, emails and verifies one-time passwords. Used by
 * {@code TwoFactorController}. Independent of {@code AuthServiceImpl} so the
 * existing password-based login flow is untouched.
 *
 * <p>OTPs are 6-digit numeric codes valid for {@value #VALIDITY_MINUTES}
 * minutes. Issuing a new OTP for the same user/purpose invalidates prior
 * unused ones. After {@value #MAX_ATTEMPTS} failed attempts a token is
 * automatically marked used.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpRepository;
    private final TwoFactorSettingRepository twoFactorRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@docvault.local}")
    private String fromEmail;

    /** Generate a fresh OTP for the user/purpose, persist it, email it. */
    @Transactional
    public OtpToken generateAndSend(User user, OtpToken.Purpose purpose) {
        otpRepository.invalidateAllForUser(user.getId(), purpose);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpToken token = OtpToken.builder()
                .code(code)
                .user(user)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(VALIDITY_MINUTES))
                .isUsed(false)
                .attempts(0)
                .build();
        OtpToken saved = otpRepository.save(token);
        sendOtpEmail(user, code, purpose);
        return saved;
    }

    /**
     * Verify an OTP for a user and purpose. Returns true on success and marks
     * the token used. Increments attempt counter on failure; after
     * {@link #MAX_ATTEMPTS} the token is also marked used.
     */
    @Transactional
    public boolean verify(User user, String code, OtpToken.Purpose purpose) {
        var opt = otpRepository.findByCodeAndUserAndPurposeAndIsUsedFalse(code, user, purpose);
        if (opt.isEmpty()) {
            // Increment attempts on the latest unused token to throttle brute force.
            otpRepository.findTopByUserAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(user, purpose)
                    .ifPresent(t -> {
                        t.setAttempts((t.getAttempts() == null ? 0 : t.getAttempts()) + 1);
                        if (t.getAttempts() >= MAX_ATTEMPTS) {
                            t.setIsUsed(true);
                        }
                        otpRepository.save(t);
                    });
            return false;
        }
        OtpToken t = opt.get();
        if (t.isExpired()) {
            t.setIsUsed(true);
            otpRepository.save(t);
            return false;
        }
        t.setIsUsed(true);
        otpRepository.save(t);

        if (purpose == OtpToken.Purpose.LOGIN_2FA || purpose == OtpToken.Purpose.SENSITIVE_ACTION) {
            twoFactorRepository.findByUser(user).ifPresent(s -> {
                s.setLastVerifiedAt(LocalDateTime.now());
                twoFactorRepository.save(s);
            });
        }
        return true;
    }

    /** Toggle 2FA on/off for the given user. */
    @Transactional
    public TwoFactorSetting setEnabled(User user, boolean enabled) {
        TwoFactorSetting setting = twoFactorRepository.findByUser(user)
                .orElseGet(() -> TwoFactorSetting.builder().user(user).enabled(false).build());
        setting.setEnabled(enabled);
        if (enabled) {
            setting.setEnabledAt(LocalDateTime.now());
        }
        return twoFactorRepository.save(setting);
    }

    public boolean isEnabled(User user) {
        return twoFactorRepository.findByUser(user)
                .map(s -> Boolean.TRUE.equals(s.getEnabled()))
                .orElse(false);
    }

    public TwoFactorSetting getOrCreateSetting(User user) {
        return twoFactorRepository.findByUser(user)
                .orElseGet(() -> twoFactorRepository.save(
                        TwoFactorSetting.builder().user(user).enabled(false).build()));
    }

    public User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Async
    protected void sendOtpEmail(User user, String code, OtpToken.Purpose purpose) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Your DocVault verification code");
            String label = switch (purpose) {
                case LOGIN_2FA -> "sign in to your account";
                case ENABLE_2FA -> "enable two-factor authentication";
                case SENSITIVE_ACTION -> "confirm a sensitive action";
            };
            String html = """
                <!DOCTYPE html>
                <html><body style="font-family:Inter,sans-serif;background:#f8fafc;padding:40px 20px;">
                  <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                    <div style="background:#2563eb;padding:24px;text-align:center;">
                      <h1 style="color:#fff;margin:0;font-size:22px;">DocVault</h1>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px;">Verification code</h2>
                      <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                        Use this code to %s. It expires in %d minutes.
                      </p>
                      <div style="font-size:32px;font-weight:700;letter-spacing:8px;background:#f1f5f9;padding:18px;border-radius:8px;text-align:center;color:#0f172a;">
                        %s
                      </div>
                      <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                        Didn't request this? Ignore this email.
                      </p>
                    </div>
                  </div>
                </body></html>
                """.formatted(label, VALIDITY_MINUTES, code);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
