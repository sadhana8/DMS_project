package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","notification_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationSetting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private Notification.NotificationType notificationType;

    @Column(name = "in_app", nullable = false) @Builder.Default
    private Boolean inApp = true;

    @Column(name = "email_enabled", nullable = false) @Builder.Default
    private Boolean email = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
