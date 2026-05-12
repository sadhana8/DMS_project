package com.dms.dto.response;

import com.dms.entity.AuditLog;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String performedBy;
    private String ipAddress;
    private AuditLog.Action action;
    private String actionLabel;
    private String entityType;
    private Long entityId;
    private String description;
    private String changeData;
    private String endpoint;
    private Integer statusCode;
    private LocalDateTime createdAt;
    private String severity; // INFO, WARNING, CRITICAL
}
