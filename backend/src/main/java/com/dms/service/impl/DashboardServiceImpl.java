package com.dms.service.impl;

import com.dms.dto.response.DashboardStatsResponse;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service that assembles the data displayed on the DocVault dashboard.
 *
 * <h2>Data provided</h2>
 * <ul>
 * <li>{@link #getStats()} – aggregate counts and storage figures for the stat
 * cards.</li>
 * <li>{@link #getRecentDocuments()} – the 10 most recently uploaded
 * documents.</li>
 * <li>{@link #getUploadTrend()} – per-day upload counts for the last 30 days
 * (currently returns mock data; replace with a real GROUP BY query for
 * production).</li>
 * <li>{@link #getStorageBreakdown()} – storage consumed per file type
 * (currently returns mock data; replace with a real aggregation query).</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * The maximum storage capacity shown on the storage gauge (10 GB).
     */
    private static final long STORAGE_LIMIT = 10L * 1024 * 1024 * 1024;

    /**
     * Assembles aggregate statistics for the dashboard stat cards.
     *
     * <p>
     * Queries executed:
     * <ul>
     * <li>{@code COUNT} of non-deleted documents.</li>
     * <li>{@code SUM(file_size)} of non-deleted documents.</li>
     * <li>{@code COUNT} of all users.</li>
     * <li>{@code COUNT} of active users.</li>
     * </ul>
     *
     * @return a {@link DashboardStatsResponse} with current aggregate values
     */
    public DashboardStatsResponse getStats() {
        long storageUsed = Optional.ofNullable(documentRepository.getTotalStorageUsed()).orElse(0L);
        return DashboardStatsResponse.builder()
                .totalDocuments(documentRepository.countActiveDocuments())
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countActiveUsers())
                .storageUsed(storageUsed)
                .storageLimit(STORAGE_LIMIT)
                .newThisMonth(0L) // TODO: add a createdAt >= startOfMonth query
                .downloadsToday(0L) // TODO: add an audit-log table and query
                .build();
    }

    /**
     * Returns the 10 most recently uploaded documents with their owner names,
     * MIME type, and file size — enough to populate the "Recent Documents"
     * list.
     *
     * @return list of maps, each representing one document's summary fields
     */
    public List<Map<String, Object>> getRecentDocuments() {
        return documentRepository.findRecentDocuments(PageRequest.of(0, 10))
                .stream()
                .map(doc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", doc.getId());
                    m.put("title", doc.getTitle());
                    m.put("originalFileName", doc.getOriginalFileName());
                    m.put("mimeType", doc.getMimeType());
                    m.put("fileSize", doc.getFileSize());
                    m.put("createdAt", doc.getCreatedAt());
                    Map<String, Object> owner = new LinkedHashMap<>();
                    owner.put("firstName", doc.getOwner().getFirstName());
                    owner.put("lastName", doc.getOwner().getLastName());
                    m.put("owner", owner);
                    return m;
                })
                .toList();
    }

    /**
     * Returns a 30-day upload-activity trend suitable for the area chart.
     *
     * <p>
     * <b>Current implementation:</b> returns pseudo-random data for each of the
     * last 30 days. Replace with a real {@code GROUP BY DATE(created_at)} query
     * before going to production.
     *
     * @return list of {@code {date, uploads}} maps ordered from oldest to
     * newest
     */
    public List<Map<String, Object>> getUploadTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDateTime day = LocalDateTime.now().minusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", String.format("%02d/%02d",
                    day.getMonthValue(), day.getDayOfMonth()));
            point.put("uploads", (int) (Math.random() * 8));
            trend.add(point);
        }
        return trend;
    }

    /**
     * Returns the storage breakdown by file type for the pie chart.
     *
     * <p>
     * <b>Current implementation:</b> returns static placeholder data. Replace
     * with a real {@code GROUP BY file_type, SUM(file_size)} query before going
     * to production.
     *
     * @return list of {@code {name, size}} maps where {@code size} is in bytes
     */
    public List<Map<String, Object>> getStorageBreakdown() {
        return List.of(
                Map.of("name", "PDF", "size", 1_200_000L),
                Map.of("name", "Images", "size", 800_000L),
                Map.of("name", "Docs", "size", 600_000L),
                Map.of("name", "Other", "size", 300_000L)
        );
    }
}
