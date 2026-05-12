package com.dms.service;

import com.dms.entity.User;
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

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to DocVault!";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                <div style="background:#2563eb;padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;font-weight:700;">DocVault</h1>
                </div>
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Welcome, %s!</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    Your DocVault account has been created. You can now securely upload, manage, and share your documents.
                  </p>
                  <a href="%s/dashboard" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Go to Dashboard
                  </a>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you didn't create this account, please ignore this email.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(user.getFirstName(), frontendUrl);
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your DocVault password";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                <div style="background:#2563eb;padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;font-weight:700;">DocVault</h1>
                </div>
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Password reset request</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 8px;">Hi %s,</p>
                  <p style="color:#475569;line-height:1.6;margin:0 0 24px;">
                    We received a request to reset your password. Click the button below. This link expires in <strong>60 minutes</strong>.
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
              </div>
            </body>
            </html>
            """.formatted(user.getFirstName(), resetLink, resetLink, resetLink);
        sendEmail(user.getEmail(), subject, html);
    }

    @Async
    public void sendShareNotificationEmail(User recipient, User sharer,
            String documentTitle, String permission) {
        String subject = sharer.getFirstName() + " shared a document with you";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                <div style="background:#2563eb;padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;font-weight:700;">DocVault</h1>
                </div>
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Document shared with you</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    <strong>%s %s</strong> shared <strong>"%s"</strong> with you with <strong>%s</strong> access.
                  </p>
                  <a href="%s/documents" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    View Document
                  </a>
                </div>
              </div>
            </body>
            </html>
            """.formatted(sharer.getFirstName(), sharer.getLastName(),
                documentTitle, permission, frontendUrl);
        sendEmail(recipient.getEmail(), subject, html);
    }

    @Async
    public void sendAccountApprovedEmail(User user) {
        String subject = "Your DocVault account has been approved";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:16px;border:1px solid #e2e8f0;overflow:hidden;">
                <div style="background:#16a34a;padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;font-weight:700;">DocVault</h1>
                </div>
                <div style="padding:36px 32px;">
                  <h2 style="color:#0f172a;font-size:20px;margin:0 0 12px;">Account Approved!</h2>
                  <p style="color:#475569;line-height:1.6;margin:0 0 20px;">
                    Hi %s, your DocVault account has been approved by an administrator.
                    You can now log in and start managing your documents.
                  </p>
                  <a href="%s/login" style="display:inline-block;background:#16a34a;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;font-size:14px;">
                    Log In Now
                  </a>
                  <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">
                    If you have any questions, please contact your administrator.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(user.getFirstName(), frontendUrl);
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
