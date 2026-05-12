package com.dms.controller;

import com.dms.dto.response.DashboardStatsResponse;
import com.dms.service.impl.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard endpoints. All four return role-scoped data — admins/managers see
 * system-wide aggregates, editors see their own data, viewers see a minimal
 * "what I can access" view.
 *
 * <ul>
 *   <li>{@code GET /api/dashboard/stats}            – headline metrics</li>
 *   <li>{@code GET /api/dashboard/recent-documents} – up to 10 recent docs</li>
 *   <li>{@code GET /api/dashboard/upload-trend}     – 30-day daily upload counts</li>
 *   <li>{@code GET /api/dashboard/storage}          – storage broken down by file type</li>
 * </ul>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> stats(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(dashboardService.getStats(ud.getUsername()));
    }

    @GetMapping("/recent-documents")
    public ResponseEntity<List<Map<String, Object>>> recentDocuments(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(dashboardService.getRecentDocuments(ud.getUsername()));
    }

    @GetMapping("/upload-trend")
    public ResponseEntity<List<Map<String, Object>>> uploadTrend(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(dashboardService.getUploadTrend(ud.getUsername()));
    }

    @GetMapping("/storage")
    public ResponseEntity<List<Map<String, Object>>> storageBreakdown(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(dashboardService.getStorageBreakdown(ud.getUsername()));
    }
}
