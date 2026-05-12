package com.dms.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsResponse {
    private long totalDocuments;
    private long archivedDocuments;
    private long newThisMonth;
    private long totalUsers;
    private long activeUsers;
    private long storageUsed;
    private long storageLimit;
    private long downloadsToday;
}
