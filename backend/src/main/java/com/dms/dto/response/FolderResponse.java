package com.dms.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shape returned by the folder endpoints. When {@link #children} and
 * {@link #documentCount} are populated, this serves as a tree node suitable
 * for rendering the folder browser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private UserResponse owner;
    private Boolean isPublic;
    private Integer documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Populated by the tree endpoint; {@code null} for flat lookups. */
    private List<FolderResponse> children;
}
