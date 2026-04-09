package com.dms.dto.response;

import com.dms.entity.Document;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO representing a document's metadata.
 *
 * <p>
 * Returned by all document read/write endpoints. Does not include the binary
 * file content — use the {@code /download} or {@code /preview} endpoints to
 * retrieve the actual file bytes.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.entity.Document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    /**
     * Database surrogate ID.
     */
    private Long id;

    /**
     * Human-readable display title shown in the UI.
     */
    private String title;

    /**
     * Optional free-text description of the document's contents.
     */
    private String description;

    /**
     * UUID-based stored file name on disk (e.g. {@code a1b2c3.pdf}). Use
     * {@link #originalFileName} for the client-facing name.
     */
    private String fileName;

    /**
     * The original file name supplied by the client at upload time.
     */
    private String originalFileName;

    /**
     * File size of the current version in bytes.
     */
    private Long fileSize;

    /**
     * Uppercase file-type extension (e.g. {@code PDF}, {@code DOCX}).
     */
    private String fileType;

    /**
     * MIME type detected by Apache Tika (e.g. {@code application/pdf}). Used by
     * the frontend to decide whether an inline preview is possible.
     */
    private String mimeType;

    /**
     * One-based current version number.
     */
    private Integer currentVersion;

    /**
     * Lifecycle status of the document. Soft-deleted documents have status
     * {@link Document.DocumentStatus#DELETED}.
     */
    private Document.DocumentStatus status;

    /**
     * The user who uploaded and owns the document.
     */
    private UserResponse owner;

    /**
     * Comma-separated tags for search and categorisation.
     */
    private String tags;

    /**
     * {@code true} when any authenticated user may access this document.
     */
    private Boolean isPublic;

    /**
     * Cumulative count of file downloads.
     */
    private Long downloadCount;

    /**
     * Cumulative count of detail-page views.
     */
    private Long viewCount;

    /**
     * Timestamp when the document record was first created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent metadata or file update.
     */
    private LocalDateTime updatedAt;
}
