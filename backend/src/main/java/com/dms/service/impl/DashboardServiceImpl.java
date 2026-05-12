package com.dms.service.impl;

import com.dms.dto.response.DashboardStatsResponse;
import com.dms.entity.*;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregates dashboard data with role-based branching.
 *
 * <ul>
 * <li>{@code ROLE_ADMIN} — system-wide totals (all documents, all users, all
 * storage, pending approvals, audit activity).</li>
 * <li>{@code ROLE_HR} — same totals as admin but without the approval queue
 * count.</li>
 * <li>{@code ROLE_ACCOUNT} — stats scoped to documents the user owns.</li>
 * <li>{@code ROLE_EMPLOYEE} — a minimal "things accessible to me" view.</li>
 * </ul>
 *
 * <p>
 * Role is decided by scanning {@code user.getRoles()} and picking the
 * highest-privilege role present (admin &gt; manager &gt; editor &gt; viewer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class DashboardServiceImpl {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final UserApprovalRepository approvalRepository;
    private final AuditLogRepository auditLogRepository;

    private static final long STORAGE_LIMIT = 10L * 1024 * 1024 * 1024; // 10 GB

    // ── Stats (role-aware) ──────────────────────────────────────────────
    public DashboardStatsResponse getStats(String email) {
        User me = userRepository.findByEmail(email).orElseThrow();
        RoleName top = topRole(me);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return switch (top) {
            case ROLE_ADMIN, ROLE_HR ->
                systemStats(startOfMonth);
            case ROLE_ACCOUNT ->
                editorStats(me, startOfMonth);
            case ROLE_EMPLOYEE ->
                viewerStats(me);
        };
    }

    private DashboardStatsResponse systemStats(LocalDateTime startOfMonth) {
        return DashboardStatsResponse.builder()
                .totalDocuments(safeCall(() -> documentRepository.countActiveDocuments()))
                .archivedDocuments(safeCall(() -> documentRepository.findDeprecated(PageRequest.of(0, 1)).getTotalElements()))
                .newThisMonth(safeCall(() -> documentRepository.countCreatedSince(startOfMonth)))
                .totalUsers(safeCall(() -> userRepository.count()))
                .activeUsers(safeCall(() -> userRepository.countActiveUsers()))
                .storageUsed(safeLong(safeCall(() -> documentRepository.getTotalStorageUsed())))
                .storageLimit(STORAGE_LIMIT)
                .downloadsToday(downloadsToday())
                .build();
    }

    private DashboardStatsResponse editorStats(User me, LocalDateTime startOfMonth) {
        return DashboardStatsResponse.builder()
                .totalDocuments(safeCall(() -> documentRepository.countByOwner(me)))
                .archivedDocuments(0)
                .newThisMonth(safeCall(() -> documentRepository.countCreatedSince(startOfMonth)))
                .totalUsers(1)
                .activeUsers(1)
                .storageUsed(safeCall(() -> documentRepository.getStorageUsedBy(me)))
                .storageLimit(STORAGE_LIMIT)
                .downloadsToday(0)
                .build();
    }

    private DashboardStatsResponse viewerStats(User me) {
        return DashboardStatsResponse.builder()
                .totalDocuments(safeCall(() -> documentRepository.findAccessibleByUser(me, PageRequest.of(0, 1)).getTotalElements()))
                .archivedDocuments(0)
                .newThisMonth(0)
                .totalUsers(1)
                .activeUsers(1)
                .storageUsed(0)
                .storageLimit(0)
                .downloadsToday(0)
                .build();
    }

    // ── Recent documents (role-aware) ───────────────────────────────────
    public List<Map<String, Object>> getRecentDocuments(String email) {
        User me = userRepository.findByEmail(email).orElseThrow();
        RoleName top = topRole(me);
        List<Document> docs = switch (top) {
            case ROLE_ADMIN, ROLE_HR ->
                documentRepository.findRecentDocuments(PageRequest.of(0, 10));
            default ->
                documentRepository
                .findAccessibleByUser(me, PageRequest.of(0, 10))
                .getContent();
        };
        return docs.stream().map(this::docSummary).toList();
    }

    private Map<String, Object> docSummary(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId());
        m.put("title", doc.getTitle());
        m.put("originalFileName", doc.getOriginalFileName());
        m.put("mimeType", doc.getMimeType());
        m.put("fileType", doc.getFileType());
        m.put("fileSize", doc.getFileSize());
        m.put("status", doc.getStatus().name());
        m.put("createdAt", doc.getCreatedAt());
        Map<String, Object> owner = new LinkedHashMap<>();
        if (doc.getOwner() != null) {
            owner.put("firstName", doc.getOwner().getFirstName());
            owner.put("lastName", doc.getOwner().getLastName());
            owner.put("email", doc.getOwner().getEmail());
        }
        m.put("owner", owner);
        return m;
    }

    // ── Upload trend: real counts by day, last 30 days ──────────────────
    public List<Map<String, Object>> getUploadTrend(String email) {
        // Simple in-memory aggregation: fetch recent docs, bucket by day.
        // For huge datasets, replace with a GROUP BY DATE_TRUNC query.
        User me = userRepository.findByEmail(email).orElseThrow();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        boolean seesAll = isAdminOrManager(me);
        List<Document> recent = seesAll
                ? documentRepository.findRecentDocuments(PageRequest.of(0, 500))
                : documentRepository.findAccessibleByUser(me, PageRequest.of(0, 500)).getContent();

        Map<String, Integer> buckets = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDateTime d = LocalDateTime.now().minusDays(i);
            buckets.put(String.format("%02d/%02d", d.getMonthValue(), d.getDayOfMonth()), 0);
        }
        for (Document d : recent) {
            if (d.getCreatedAt() == null || d.getCreatedAt().isBefore(cutoff)) {
                continue;
            }
            String k = String.format("%02d/%02d",
                    d.getCreatedAt().getMonthValue(), d.getCreatedAt().getDayOfMonth());
            buckets.merge(k, 1, Integer::sum);
        }

        List<Map<String, Object>> out = new ArrayList<>(buckets.size());
        buckets.forEach((k, v) -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", k);
            point.put("uploads", v);
            out.add(point);
        });
        return out;
    }

    // ── Storage breakdown: real SUM by file type ────────────────────────
    public List<Map<String, Object>> getStorageBreakdown(String email) {
        User me = userRepository.findByEmail(email).orElseThrow();
        if (!isAdminOrManager(me)) {
            // Editors/Viewers just get their own total.
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", "My documents");
            row.put("size", documentRepository.getStorageUsedBy(me));
            return List.of(row);
        }
        List<Object[]> rows = documentRepository.groupByFileType();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r[0] != null ? r[0].toString() : "OTHER");
            m.put("count", r[1]);
            m.put("size", r[2]);
            out.add(m);
        }
        return out;
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    private long downloadsToday() {
        return safeCall(() -> {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            return auditLogRepository.search(null, AuditLog.Action.DOCUMENT_DOWNLOAD,
                    null, startOfDay, null, PageRequest.of(0, 1)).getTotalElements();
        });
    }

    /**
     * Run a query that returns a Long-ish value, returning 0 if it throws.
     * Without this, a single bad query (e.g. a stale enum CHECK constraint or
     * an empty-table SUM that returns null) would 500 the whole endpoint.
     */
    private long safeCall(java.util.concurrent.Callable<? extends Number> q) {
        try {
            Number v = q.call();
            return v == null ? 0L : v.longValue();
        } catch (Exception e) {
            log.warn("Dashboard stat query failed: {}", e.getMessage());
            return 0L;
        }
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private boolean isAdminOrManager(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN
                || r.getName() == RoleName.ROLE_HR);
    }

    /**
     * Picks the highest-privilege role on the user, defaulting to VIEWER.
     */
    private RoleName topRole(User user) {
        Set<RoleName> names = new HashSet<>();
        user.getRoles().forEach(r -> names.add(r.getName()));
        if (names.contains(RoleName.ROLE_ADMIN)) {
            return RoleName.ROLE_ADMIN;
        }
        if (names.contains(RoleName.ROLE_HR)) {
            return RoleName.ROLE_HR;
        }
        if (names.contains(RoleName.ROLE_ACCOUNT)) {
            return RoleName.ROLE_ACCOUNT;
        }
        return RoleName.ROLE_EMPLOYEE;
    }
}
