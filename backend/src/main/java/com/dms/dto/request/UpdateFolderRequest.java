package com.dms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code PUT /api/folders/{id}}. All fields are optional; a
 * {@code null} field means "leave unchanged". To move a folder, set
 * {@link #parentId} to the new parent's ID (or a negative number to make it
 * a root folder — backend treats any negative value as "clear parent").
 */
@Data
public class UpdateFolderRequest {

    @Size(min = 1, max = 150)
    private String name;

    private String description;

    /** New parent ID. {@code null} = unchanged; negative value = make root. */
    private Long parentId;

    private Boolean isPublic;
}
