package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/folders}.
 *
 * <p>A folder belongs to exactly one owner (the authenticated caller) and
 * optionally has a parent folder. If {@code parentId} is {@code null} the
 * folder is created at the root of that user's folder tree.
 */
@Data
public class CreateFolderRequest {

    /** Display name shown in the UI. Required, 1–150 chars. */
    @NotBlank
    @Size(min = 1, max = 150)
    private String name;

    /** Optional free-form description. */
    private String description;

    /** Parent folder ID, or {@code null} for a root folder. */
    private Long parentId;

    /** If true, the folder's documents are visible to all authenticated users. */
    private Boolean isPublic;
}
