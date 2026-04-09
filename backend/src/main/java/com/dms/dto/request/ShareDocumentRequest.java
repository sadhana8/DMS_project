package com.dms.dto.request;

import com.dms.entity.DocumentPermission;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for {@code POST /api/documents/{id}/permissions}.
 *
 * <p>
 * Used to grant a specific user access to a document at a chosen permission
 * level. If the user already has an entry, it is updated.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see DocumentPermission.PermissionType
 */
@Data
public class ShareDocumentRequest {

    /**
     * E-mail address of the user to grant access to. Must match an existing
     * registered user; otherwise
     * {@link com.dms.exception.ResourceNotFoundException} is thrown.
     */
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Recipient email is required")
    private String email;

    /**
     * The level of access to grant. One of {@code VIEW}, {@code DOWNLOAD},
     * {@code EDIT}, or {@code ADMIN}.
     */
    @NotNull(message = "Permission level is required")
    private DocumentPermission.PermissionType permission;

    /**
     * Optional ISO-8601 date-time string after which the permission expires.
     * {@code null} means the permission never expires. Parsed and converted to
     * {@link java.time.LocalDateTime} by the service layer.
     */
    private String expiresAt;
}
