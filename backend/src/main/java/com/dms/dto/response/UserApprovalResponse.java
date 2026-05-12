package com.dms.dto.response;

import com.dms.entity.UserApproval;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserApprovalResponse {
    private Long id;
    private UserResponse user;
    private UserApproval.ApprovalStatus status;
    private String reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
