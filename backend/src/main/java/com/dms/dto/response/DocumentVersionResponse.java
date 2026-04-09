package com.dms.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO representing a single entry in a document's version history.
 *
 * <p>
 * Returned by {@code GET /api/documents/{id}/versions} and
 * {@code POST /api/documents/{id}/versions}.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.entity.DocumentVersion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponse {

    /**
     * Database surrogate ID of the version record.
     */
    private Long id;

    /**
     * One-based sequential version number. Version {@code 1} is always the
     * initial upload.
     */
    private Integer versionNumber;

    /**
     * UUID-based stored file name for this version.
     */
    private String fileName;

    /**
     * File size of this version in bytes.
     */
    private Long fileSize;

    /**
     * Optional human-readable description of what changed in this version,
     * entered by the uploader at upload time.
     */
    private String changeSummary;

    /**
     * The user who uploaded this version; {@code null} if the uploader was
     * deleted.
     */
    private UserResponse uploadedBy;

    /**
     * Timestamp when this version was created.
     */
    private LocalDateTime createdAt;
}
