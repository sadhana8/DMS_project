package com.dms.dto.request;

import com.dms.entity.Notification;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationSettingRequest {
    @NotNull private Notification.NotificationType type;
    private Boolean inApp;
    private Boolean email;
}
