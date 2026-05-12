package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
            @Index(name = "idx_notif_user", columnList = "user_id"),
            @Index(name = "idx_notif_read", columnList = "is_read"),
            @Index(name = "idx_notif_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /**
     * Relative deep-link, e.g. /documents/42
     */
    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        DOCUMENT_SHARED, DOCUMENT_DEPRECATED, DOCUMENT_RESTORED,
        VERSION_UPLOADED, ROLE_CHANGED, USER_APPROVED, USER_REJECTED,
        ACCOUNT_DEPRECATED, ACCOUNT_RESTORED, PENDING_APPROVAL,
        SYSTEM, MENTION
    }
}
