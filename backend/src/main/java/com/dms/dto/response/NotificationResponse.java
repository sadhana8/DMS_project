package com.dms.dto.response;

import com.dms.entity.Notification;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Notification.NotificationType type;
    private String typeLabel;
    private String colour;
    private String title;
    private String message;
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
