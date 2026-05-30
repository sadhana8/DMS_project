package com.dms.service.impl;

import com.dms.entity.*;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.*;
import com.dms.service.EmailService;
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
 * Generates, emails and verifies one-time passwords. Uses the unified AppToken table
 * (token_type = OTP_*) while keeping the legacy OtpToken table in sync for any
 * components that haven't been migrated yet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS     = 5;
    private static final SecureRandom RANDOM  = new SecureRandom();

    private final OtpTokenRepository     otpRepository;
    private final AppTokenRepository     appTokenRepository;
    private final TwoFactorSettingRepository twoFactorRepository;
    private final UserRepository         userRepository;
    private final JavaMailSender         mailSender;
    private final EmailService           emailService;

    @Value("${app.mail.from:noreply@docvault.local}")
    private String fromEmail;

    private AppToken.TokenType toAppType(OtpToken.Purpose purpose) {
        return switch (purpose) {
            case LOGIN_2FA       -> AppToken.TokenType.OTP_LOGIN_2FA;
            case ENABLE_2FA      -> AppToken.TokenType.OTP_ENABLE_2FA;
            case SENSITIVE_ACTION-> AppToken.TokenType.OTP_SENSITIVE_ACTION;
        };
    }

    @Transactional
    public OtpToken generateAndSend(User user, OtpToken.Purpose purpose) {
        // Invalidate old OTPs in legacy table
        otpRepository.invalidateAllForUser(user.getId(), purpose);
        // Invalidate in unified table too
        appTokenRepository.deleteByUserAndType(user, toAppType(purpose));

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(VALIDITY_MINUTES);

        // Persist in legacy table
        OtpToken token = OtpToken.builder()
                .code(code).user(user).purpose(purpose)
                .expiresAt(expiresAt).isUsed(false).attempts(0).build();
        OtpToken saved = otpRepository.save(token);

        // Also persist in unified table
        appTokenRepository.save(AppToken.builder()
                .tokenValue(code)
                .tokenType(toAppType(purpose))
                .user(user)
                .expiresAt(expiresAt)
                .build());

        sendOtpEmail(user, code, purpose);
        return saved;
    }

    @Transactional
    public boolean verify(User user, String code, OtpToken.Purpose purpose) {
        var opt = otpRepository.findByCodeAndUserAndPurposeAndIsUsedFalse(code, user, purpose);
        if (opt.isEmpty()) {
            otpRepository.findTopByUserAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(user, purpose)
                    .ifPresent(t -> {
                        t.setAttempts((t.getAttempts() == null ? 0 : t.getAttempts()) + 1);
                        if (t.getAttempts() >= MAX_ATTEMPTS) t.setIsUsed(true);
                        otpRepository.save(t);
                    });
            return false;
        }
        OtpToken t = opt.get();
        if (t.isExpired()) { t.setIsUsed(true); otpRepository.save(t); return false; }
        t.setIsUsed(true);
        otpRepository.save(t);
        // Mark used in unified table too
        appTokenRepository.findByTokenValueAndTokenType(code, toAppType(purpose))
                .ifPresent(at -> { at.setIsUsed(true); appTokenRepository.save(at); });

        if (purpose == OtpToken.Purpose.LOGIN_2FA || purpose == OtpToken.Purpose.SENSITIVE_ACTION) {
            twoFactorRepository.findByUser(user).ifPresent(s -> {
                s.setLastVerifiedAt(LocalDateTime.now()); twoFactorRepository.save(s);
            });
        }
        return true;
    }

    @Transactional
    public TwoFactorSetting setEnabled(User user, boolean enabled) {
        TwoFactorSetting setting = twoFactorRepository.findByUser(user)
                .orElseGet(() -> TwoFactorSetting.builder().user(user).enabled(false).build());
        setting.setEnabled(enabled);
        if (enabled) setting.setEnabledAt(LocalDateTime.now());
        return twoFactorRepository.save(setting);
    }

    public boolean isEnabled(User user) {
        return twoFactorRepository.findByUser(user)
                .map(s -> Boolean.TRUE.equals(s.getEnabled())).orElse(false);
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
            String label = switch (purpose) {
                case LOGIN_2FA        -> "sign in to your account";
                case ENABLE_2FA       -> "enable two-factor authentication";
                case SENSITIVE_ACTION -> "confirm a sensitive action";
            };
            // Try using the full EmailService first (which has company branding).
            // Construct an inline HTML email here so we don't add a method to EmailService
            // that would couple it to OTP internals.
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            // Obtain company name dynamically if possible
            String companyName = "DocVault";
            try {
                Object es = emailService;
                java.lang.reflect.Method m = es.getClass().getMethod("companyName");
                m.setAccessible(true);
                companyName = (String) m.invoke(es);
            } catch (Exception ignored) {}
            helper.setSubject("Your " + companyName + " verification code");
            String html = """
                <!DOCTYPE html>
                <html><body style="font-family:Inter,sans-serif;background:#f8fafc;padding:40px 20px;">
                  <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                    <div style="background:#2563eb;padding:24px;text-align:center;">
                      <h1 style="color:#fff;margin:0;font-size:22px;">%s</h1>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px;">Verification code</h2>
                      <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                        Use this code to %s. It expires in %d minutes.
                      </p>
                      <div style="font-size:36px;font-weight:700;letter-spacing:10px;background:#f1f5f9;padding:20px;border-radius:8px;text-align:center;color:#0f172a;">
                        %s
                      </div>
                      <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                        Didn't request this? Ignore this email. Your account is safe.
                      </p>
                    </div>
                    <div style="background:#f1f5f9;padding:16px;text-align:center;border-top:1px solid #e2e8f0;">
                      <p style="color:#94a3b8;font-size:11px;margin:0;">&copy; %s. Automated message — do not reply.</p>
                    </div>
                  </div>
                </body></html>
                """.formatted(companyName, label, VALIDITY_MINUTES, code, companyName);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
