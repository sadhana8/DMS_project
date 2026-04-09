package com.dms.dto.response;

import com.dms.entity.DocumentPermission;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO representing a single access-control entry for a document.
 *
 * <p>
 * Returned by the permissions endpoints:  {@code GET /api/documents/{id}/permissions},
 * {@code POST /api/documents/{id}/permissions}, and
 * {@code PUT /api/documents/{id}/permissions/{userId}}.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.entity.DocumentPermission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPermissionResponse {

    /**
     * Database surrogate ID of the permission entry.
     */
    private Long id;

    /**
     * The user who has been granted access.
     */
    private UserResponse user;

    /**
     * The level of access granted. One of {@code VIEW}, {@code DOWNLOAD},
     * {@code EDIT}, or {@code ADMIN}.
     */
    private DocumentPermission.PermissionType permission;

    /**
     * The date/time after which this permission is no longer valid.
     * {@code null} means the permission never expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Timestamp when this permission entry was first created.
     */
    private LocalDateTime grantedAt;

    /**
     * The user who granted this permission. {@code null} if the granting user
     * has since been deleted.
     */
    private UserResponse grantedBy;
}
