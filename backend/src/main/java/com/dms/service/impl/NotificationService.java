package com.dms.service.impl;

import com.dms.dto.request.UpdateNotificationSettingRequest;
import com.dms.dto.response.*;
import com.dms.entity.*;
import com.dms.entity.Notification.NotificationType;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final NotificationSettingRepository settingRepo;
    private final UserRepository userRepo;

    // Map.of() has overloads only up to 10 pairs; 11 entries require Map.ofEntries
    private static final Map<NotificationType, String> LABELS = Map.ofEntries(
            Map.entry(NotificationType.DOCUMENT_SHARED, "Document Shared"),
            Map.entry(NotificationType.DOCUMENT_DEPRECATED, "Document Deprecated"),
            Map.entry(NotificationType.DOCUMENT_RESTORED, "Document Restored"),
            Map.entry(NotificationType.VERSION_UPLOADED, "New Version"),
            Map.entry(NotificationType.ROLE_CHANGED, "Role Changed"),
            Map.entry(NotificationType.USER_APPROVED, "Account Approved"),
            Map.entry(NotificationType.USER_REJECTED, "Account Rejected"),
            Map.entry(NotificationType.ACCOUNT_DEPRECATED, "Account Deprecated"),
            Map.entry(NotificationType.ACCOUNT_RESTORED, "Account Restored"),
            Map.entry(NotificationType.SYSTEM, "System Notice"),
            Map.entry(NotificationType.MENTION, "Mention"),
            Map.entry(NotificationType.PENDING_APPROVAL, "New Registration")
    );
    private static final Map<NotificationType, String> COLOURS = Map.ofEntries(
            Map.entry(NotificationType.DOCUMENT_SHARED, "blue"),
            Map.entry(NotificationType.DOCUMENT_DEPRECATED, "amber"),
            Map.entry(NotificationType.DOCUMENT_RESTORED, "green"),
            Map.entry(NotificationType.VERSION_UPLOADED, "purple"),
            Map.entry(NotificationType.ROLE_CHANGED, "blue"),
            Map.entry(NotificationType.USER_APPROVED, "green"),
            Map.entry(NotificationType.USER_REJECTED, "red"),
            Map.entry(NotificationType.ACCOUNT_DEPRECATED, "red"),
            Map.entry(NotificationType.ACCOUNT_RESTORED, "green"),
            Map.entry(NotificationType.SYSTEM, "gray"),
            Map.entry(NotificationType.MENTION, "pink"),
            Map.entry(NotificationType.PENDING_APPROVAL, "amber")
    );
    private static final Map<NotificationType, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(NotificationType.DOCUMENT_SHARED, "When someone shares a document with you"),
            Map.entry(NotificationType.DOCUMENT_DEPRECATED, "When a document you own is deprecated"),
            Map.entry(NotificationType.DOCUMENT_RESTORED, "When a deprecated document is restored"),
            Map.entry(NotificationType.VERSION_UPLOADED, "When a new version is uploaded"),
            Map.entry(NotificationType.ROLE_CHANGED, "When your roles are changed by admin"),
            Map.entry(NotificationType.USER_APPROVED, "When your account is approved by admin"),
            Map.entry(NotificationType.USER_REJECTED, "When your account registration is rejected"),
            Map.entry(NotificationType.ACCOUNT_DEPRECATED, "When your account is deprecated"),
            Map.entry(NotificationType.ACCOUNT_RESTORED, "When your account is restored"),
            Map.entry(NotificationType.SYSTEM, "System announcements"),
            Map.entry(NotificationType.MENTION, "When you are mentioned in a comment"),
            Map.entry(NotificationType.PENDING_APPROVAL, "When a new user registers and needs approval (admin only)")
    );

    // ── Create ────────────────────────────────────────────────────────────
    @Transactional
    public void create(String recipientEmail, NotificationType type,
            String title, String message, String link) {
        User recipient = userRepo.findByEmail(recipientEmail).orElse(null);
        if (recipient == null || !isInAppEnabled(recipient.getId(), type)) {
            return;
        }
        notifRepo.save(Notification.builder()
                .recipient(recipient).type(type)
                .title(title).message(message).link(link).build());
    }

    @Transactional
    public void createForUser(Long userId, NotificationType type,
            String title, String message, String link) {
        User recipient = userRepo.findById(userId).orElse(null);
        if (recipient == null || !isInAppEnabled(userId, type)) {
            return;
        }
        notifRepo.save(Notification.builder()
                .recipient(recipient).type(type)
                .title(title).message(message).link(link).build());
    }

    /**
     * Fan-out: create the same notification for every active admin. Used for
     * "new user needs approval" alerts. Skips admins who have opted out of this
     * notification type via their settings.
     */
    @Transactional
    public void notifyAllAdmins(NotificationType type, String title, String message, String link) {
        var admins = userRepo.findAllActiveByRole(com.dms.entity.RoleName.ROLE_ADMIN);
        for (User admin : admins) {
            if (!isInAppEnabled(admin.getId(), type)) {
                continue;
            }
            notifRepo.save(Notification.builder()
                    .recipient(admin).type(type)
                    .title(title).message(message).link(link).build());
        }
    }

    // ── Read with filters ─────────────────────────────────────────────────
    public Page<NotificationResponse> getFiltered(String email, String type, Boolean isRead,
            LocalDateTime from, LocalDateTime to,
            int page, int size) {
        User user = userRepo.findByEmail(email).orElseThrow();
        NotificationType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = NotificationType.valueOf(type);
            } catch (Exception ignored) {
            }
        }
        return notifRepo.findFiltered(user.getId(), typeEnum, isRead, from, to,
                PageRequest.of(page, size)).map(this::toResponse);
    }

    public long getUnreadCount(String email) {
        return userRepo.findByEmail(email).map(u -> notifRepo.countUnread(u.getId())).orElse(0L);
    }

    @Transactional
    public void markAllRead(String email) {
        userRepo.findByEmail(email).ifPresent(u -> notifRepo.markAllRead(u.getId()));
    }

    @Transactional
    public void markOneRead(Long id, String email) {
        userRepo.findByEmail(email).ifPresent(u -> notifRepo.markOneRead(id, u.getId()));
    }

    // ── Settings ──────────────────────────────────────────────────────────
    public List<NotificationSettingResponse> getSettings(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        Map<NotificationType, NotificationSetting> saved = new EnumMap<>(NotificationType.class);
        settingRepo.findByUserId(user.getId()).forEach(s -> saved.put(s.getNotificationType(), s));
        List<NotificationSettingResponse> result = new ArrayList<>();
        for (NotificationType t : NotificationType.values()) {
            NotificationSetting row = saved.get(t);
            result.add(NotificationSettingResponse.builder()
                    .type(t)
                    .typeLabel(LABELS.getOrDefault(t, t.name()))
                    .description(DESCRIPTIONS.getOrDefault(t, ""))
                    .inApp(row != null ? row.getInApp() : true)
                    .email(row != null ? row.getEmail() : true)
                    .build());
        }
        return result;
    }

    @Transactional
    public NotificationSettingResponse updateSetting(String email, UpdateNotificationSettingRequest req) {
        User user = userRepo.findByEmail(email).orElseThrow();
        NotificationSetting row = settingRepo.findByUserIdAndNotificationType(user.getId(), req.getType())
                .orElse(NotificationSetting.builder().user(user).notificationType(req.getType()).build());
        if (req.getInApp() != null) {
            row.setInApp(req.getInApp());
        }
        if (req.getEmail() != null) {
            row.setEmail(req.getEmail());
        }
        settingRepo.save(row);
        return NotificationSettingResponse.builder()
                .type(row.getNotificationType())
                .typeLabel(LABELS.getOrDefault(row.getNotificationType(), row.getNotificationType().name()))
                .description(DESCRIPTIONS.getOrDefault(row.getNotificationType(), ""))
                .inApp(row.getInApp()).email(row.getEmail()).build();
    }

    @Transactional
    public void resetSettings(String email) {
        userRepo.findByEmail(email).ifPresent(u -> settingRepo.deleteByUserId(u.getId()));
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanup() {
        notifRepo.deleteOldRead(LocalDateTime.now().minusDays(30));
    }

    public boolean isInAppEnabled(Long userId, NotificationType type) {
        return settingRepo.findByUserIdAndNotificationType(userId, type)
                .map(NotificationSetting::getInApp).orElse(true);
    }

    public boolean isEmailEnabled(Long userId, NotificationType type) {
        return settingRepo.findByUserIdAndNotificationType(userId, type)
                .map(NotificationSetting::getEmail).orElse(true);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).type(n.getType())
                .typeLabel(LABELS.getOrDefault(n.getType(), n.getType().name()))
                .colour(COLOURS.getOrDefault(n.getType(), "gray"))
                .title(n.getTitle()).message(n.getMessage())
                .link(n.getLink()).isRead(n.getIsRead())
                .createdAt(n.getCreatedAt()).build();
    }
}
