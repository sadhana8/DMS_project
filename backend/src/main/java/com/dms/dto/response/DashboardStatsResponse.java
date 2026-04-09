package com.dms.dto.response;

import lombok.*;

/**
 * Response DTO carrying the aggregate statistics shown on the dashboard page.
 *
 * <p>
 * Returned by {@code GET /api/dashboard/stats}. All numeric values represent
 * system-wide totals, not per-user figures.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.service.impl.DashboardServiceImpl#getStats()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /**
     * Total number of non-deleted documents across all users.
     */
    private long totalDocuments;

    /**
     * Number of documents with status {@code ARCHIVED}.
     */
    private long archivedDocuments;

    /**
     * Number of new documents uploaded in the current calendar month. Currently
     * always {@code 0} — implement with a real query when needed.
     */
    private long newThisMonth;

    /**
     * Total number of registered user accounts.
     */
    private long totalUsers;

    /**
     * Number of user accounts with {@code isActive = true}.
     */
    private long activeUsers;

    /**
     * Sum of all non-deleted document file sizes in bytes. Used to render the
     * storage usage gauge on the dashboard.
     */
    private long storageUsed;

    /**
     * Configured storage capacity ceiling in bytes. Default: 10 GB
     * ({@code 10 × 1024 × 1024 × 1024}).
     */
    private long storageLimit;

    /**
     * Total downloads recorded today across all documents. Currently always
     * {@code 0} — implement with an audit-log table when needed.
     */
    private long downloadsToday;
}
