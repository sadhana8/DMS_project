package com.dms.dto.request;

import com.dms.entity.DocumentPermission;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ShareDocumentRequest {
    @Email @NotBlank
    private String email;
    @NotNull
    private DocumentPermission.PermissionType permission;
    private String expiresAt;
}
