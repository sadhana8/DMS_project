package com.dms.dto.response;

import com.dms.entity.Notification;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationSettingResponse {
    private Notification.NotificationType type;
    private String typeLabel;
    private String description;
    private Boolean inApp;
    private Boolean email;
}
