package com.dms.dto.request;

import lombok.Data;

/**
 * Request DTO for {@code PUT /api/documents/{id}}.
 *
 * <p>
 * All fields are optional; only non-null values are applied to the existing
 * document record (partial update semantics). The file binary cannot be changed
 * through this endpoint — use the versioning endpoints
 * ({@code POST /api/documents/{id}/versions}) instead.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class UpdateDocumentRequest {

    /**
     * New display title. {@code null} leaves the existing value unchanged.
     */
    private String title;

    /**
     * New free-text description. {@code null} leaves the existing value
     * unchanged.
     */
    private String description;

    /**
     * New comma-separated tags (e.g. {@code "finance,Q3,2024"}). {@code null}
     * leaves existing tags unchanged.
     */
    private String tags;

    /**
     * When {@code true}, the document becomes visible to all authenticated
     * users. {@code null} leaves the existing visibility unchanged.
     */
    private Boolean isPublic;

    /**
     * New lifecycle status string. Must match one of the
     * {@link com.dms.entity.Document.DocumentStatus} constant names (e.g.
     * {@code "ARCHIVED"}). Invalid values are silently ignored. {@code null}
     * leaves the status unchanged.
     */
    private String status;
}
