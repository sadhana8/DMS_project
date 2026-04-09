package com.dms.controller;

import com.dms.dto.response.DashboardStatsResponse;
import com.dms.service.impl.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller that serves all data required by the DocVault dashboard page.
 *
 * <p>
 * Base path: {@code /api/dashboard}
 *
 * <p>
 * All endpoints require an authenticated user (any role). The data returned is
 * system-wide aggregate data rather than user-specific data.
 *
 * <h2>Endpoint summary</h2>
 * <ul>
 * <li>{@code GET /stats} – aggregate counts for the stat cards.</li>
 * <li>{@code GET /recent-documents} – the 10 most recently uploaded
 * documents.</li>
 * <li>{@code GET /upload-trend} – per-day upload counts for the last 30
 * days.</li>
 * <li>{@code GET /storage} – storage breakdown by file type.</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see DashboardServiceImpl
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /**
     * Delegate for all dashboard data-assembly logic.
     */
    private final DashboardServiceImpl dashboardService;

    /**
     * Returns the aggregate statistics displayed in the dashboard stat cards:
     * total documents, storage used, total and active users, new documents this
     * month, and downloads today.
     *
     * <p>
     * {@code GET /api/dashboard/stats}
     *
     * @return {@code 200 OK} with a {@link DashboardStatsResponse}
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> stats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    /**
     * Returns the 10 most recently uploaded documents with their title, MIME
     * type, file size, owner name, and creation date. Used to populate the
     * "Recent Documents" section of the dashboard.
     *
     * <p>
     * {@code GET /api/dashboard/recent-documents}
     *
     * @return {@code 200 OK} with a list of document summary maps
     */
    @GetMapping("/recent-documents")
    public ResponseEntity<List<Map<String, Object>>> recentDocuments() {
        return ResponseEntity.ok(dashboardService.getRecentDocuments());
    }

    /**
     * Returns per-day upload counts for the last 30 calendar days. Used to
     * render the area chart on the dashboard.
     *
     * <p>
     * {@code GET /api/dashboard/upload-trend}
     *
     * <p>
     * Response shape (one entry per day):
     * <pre>{@code [{ "date": "03/15", "uploads": 4 }, ...]}</pre>
     *
     * @return {@code 200 OK} with a list of {@code {date, uploads}} maps
     */
    @GetMapping("/upload-trend")
    public ResponseEntity<List<Map<String, Object>>> uploadTrend() {
        return ResponseEntity.ok(dashboardService.getUploadTrend());
    }

    /**
     * Returns the storage consumption broken down by file type. Used to render
     * the pie chart on the dashboard.
     *
     * <p>
     * {@code GET /api/dashboard/storage}
     *
     * <p>
     * Response shape:
     * <pre>{@code [{ "name": "PDF", "size": 1200000 }, ...]}</pre>
     *
     * @return {@code 200 OK} with a list of {@code {name, size}} maps where
     * {@code size} is in bytes
     */
    @GetMapping("/storage")
    public ResponseEntity<List<Map<String, Object>>> storageBreakdown() {
        return ResponseEntity.ok(dashboardService.getStorageBreakdown());
    }
}
