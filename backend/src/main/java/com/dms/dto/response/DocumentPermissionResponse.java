package com.dms.dto.response;

import com.dms.entity.DocumentPermission;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentPermissionResponse {
    private Long id;
    private UserResponse user;
    private DocumentPermission.PermissionType permission;
    private LocalDateTime expiresAt;
    private LocalDateTime grantedAt;
    private UserResponse grantedBy;
}
