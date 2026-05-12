package com.dms.dto.response;

import com.dms.entity.Document;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private Integer currentVersion;
    private Document.DocumentStatus status;
    private UserResponse owner;
    private String tags;
    private Boolean isPublic;
    private Long downloadCount;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
