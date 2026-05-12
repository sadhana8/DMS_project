package com.dms.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentVersionResponse {
    private Long id;
    private Integer versionNumber;
    private String fileName;
    private Long fileSize;
    private String changeSummary;
    private UserResponse uploadedBy;
    private LocalDateTime createdAt;
}
