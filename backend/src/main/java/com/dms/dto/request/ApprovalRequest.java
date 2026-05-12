package com.dms.dto.request;

import com.dms.entity.UserApproval;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequest {
    @NotNull private UserApproval.ApprovalStatus status;
    private String note;
}
