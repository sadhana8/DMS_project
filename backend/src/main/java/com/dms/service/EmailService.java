package com.dms.service;

import com.dms.entity.User;
import com.dms.service.impl.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SettingsService settingsService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /** Returns the company name from settings, falling back to app_name then "DocVault" */
    private String companyName() {
        try {
            String cn = settingsService.get("company_name");
            if (cn != null && !cn.isBlank()) return cn;
            String an = settingsService.get("app_name");
            if (an != null && !an.isBlank()) return an;
        } catch (Exception ignored) {}
        return "DocVault";
    }

    private String emailHeader(String bgColor) {
        String name = companyName();
        return """
            <div style="background:%s;padding:32px;text-align:center;">
              <h1 style="color:#fff;margin:0;font-size:24px;font-weight:700;">%s</h1>
            </div>
            """.formatted(bgColor, name);
    }

    private String emailFooter() {
        String name = companyName();
        return """
            <div style="background:#f1f5f9;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;">
              <p style="color:#94a3b8;font-size:11px;margin:0;">
                &copy; %s. This is an automated message from %s Document Management System.
              </p>
            </div>
            """.formatted(java.time.Year.now().getValue(), name);
    }

    @Async
    public void sendWelcomeEmail(User user) {
        String name = companyName();
        String subject = "Welcome to " + name + "!";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Welcome, %s!</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    Your %s account has been created. You can now securely upload, manage, and share your documents.
                  </p>
                  <a href="%s/dashboard" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Go to Dashboard
                  </a>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you didn't create this account, please ignore this email.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#2563eb"), user.getFirstName(), name, frontendUrl, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String name = companyName();
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your " + name + " password";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Password reset request</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 8px;">Hi %s,</p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 24px;">
                    We received a request to reset your %s password. Click the button below. This link expires in <strong>60 minutes</strong>.
                  </p>
                  <a href="%s" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Reset Password
                  </a>
                  <p style="color:#475569;line-height:1.6;margin:24px 0 0;font-size:13px;">
                    Or copy this link:<br/>
                    <a href="%s" style="color:#2563eb;word-break:break-all;">%s</a>
                  </p>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you didn't request a password reset, please ignore this email.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#2563eb"), user.getFirstName(), name, resetLink, resetLink, resetLink, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendShareNotificationEmail(User recipient, User sharer,
            String documentTitle, String permission) {
        String name = companyName();
        String subject = sharer.getFirstName() + " shared a document with you on " + name;
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Document shared with you</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    <strong>%s %s</strong> shared <strong>"%s"</strong> with you with <strong>%s</strong> access on <strong>%s</strong>.
                  </p>
                  <a href="%s/documents" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    View Document
                  </a>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#2563eb"), sharer.getFirstName(), sharer.getLastName(),
                documentTitle, permission, name, frontendUrl, emailFooter());
        sendEmail(recipient.getEmail(), subject, html);
    }

    @Async
    public void sendAccountApprovedEmail(User user) {
        String name = companyName();
        String subject = "Your " + name + " account has been approved";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Account Approved!</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    Hi %s, your %s account has been approved by an administrator.
                    You can now log in and start managing your documents.
                  </p>
                  <a href="%s/login" style="display:inline-block;background:#16a34a;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Log In Now
                  </a>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you have any questions, please contact your administrator.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#16a34a"), user.getFirstName(), name, frontendUrl, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendAdminCreatedAccountEmail(User user, String tempPassword) {
        String name = companyName();
        String subject = "Your " + name + " account has been created";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Welcome, %s!</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 12px;">
                    An administrator has created a %s account for you. Your temporary login credentials are:
                  </p>
                  <div style="background:#f1f5f9;border-radius:10px;padding:16px;margin:18px 0;">
                    <p style="margin:0 0 6px;color:#475569;"><strong>Email:</strong> %s</p>
                    <p style="margin:0;color:#475569;"><strong>Temporary password:</strong>
                      <code style="background:#fff;padding:4px 10px;border-radius:6px;color:#dc2626;font-size:14px;">%s</code></p>
                  </div>
                  <p style="color:#dc2626;line-height:1.6;margin:0 0 20px;font-size:13px;">
                    <strong>Important:</strong> You will be required to change this password the first time you log in.
                  </p>
                  <a href="%s/login" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Log In
                  </a>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you weren't expecting this email, please contact your administrator.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#2563eb"), user.getFirstName(), name, user.getEmail(), tempPassword, frontendUrl, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendAccountRejectedEmail(User user, String reason) {
        String name = companyName();
        String subject = "Your " + name + " registration was not approved";
        String safeReason = reason == null || reason.isBlank() ? "No reason was provided." : reason;
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Registration not approved</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 12px;">Hi %s,</p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 16px;">
                    Unfortunately, your %s registration request was not approved.
                  </p>
                  <div style="background:#fef2f2;border-left:4px solid #dc2626;border-radius:6px;padding:14px 18px;margin:0 0 18px;">
                    <p style="margin:0;color:#7f1d1d;font-size:13px;"><strong>Reason:</strong> %s</p>
                  </div>
                  <p style="color:#475569;line-height:1.6;margin:0 0 8px;font-size:13px;">
                    If you believe this was a mistake, please contact your administrator.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#dc2626"), user.getFirstName(), name, safeReason, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendTerminationEmail(User user, String reason, String terminatedBy) {
        String name = companyName();
        String subject = "Your " + name + " account has been terminated";
        String safeReason = reason == null || reason.isBlank() ? "No reason was provided." : reason;
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Account Terminated</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 16px;">Hi %s,</p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 16px;">
                    Your %s account access has been terminated by %s, effective immediately.
                  </p>
                  <div style="background:#fef2f2;border-left:4px solid #dc2626;border-radius:6px;padding:14px 18px;margin:0 0 18px;">
                    <p style="margin:0;color:#7f1d1d;font-size:13px;"><strong>Reason:</strong> %s</p>
                  </div>
                  <p style="color:#475569;line-height:1.6;margin:0;font-size:13px;">
                    If you have questions, please contact your administrator.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#dc2626"), user.getFirstName(), name, terminatedBy, safeReason, emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendResignationConfirmedEmail(User user, java.time.LocalDateTime effectiveDate) {
        String name = companyName();
        String subject = "Resignation acknowledged — " + name + " access will end on " +
            effectiveDate.toLocalDate();
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                %s
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Resignation acknowledged</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 16px;">Hi %s,</p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 16px;">
                    Your resignation has been recorded. Your %s access will be revoked
                    at end of day on <strong>%s</strong>.
                  </p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 8px;font-size:13px;">
                    Please ensure you've downloaded any personal documents before that date.
                    Thank you for your service.
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(emailHeader("#2563eb"), user.getFirstName(), name, effectiveDate.toLocalDate(), emailFooter());
        sendEmail(user.getEmail(), subject, html);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
