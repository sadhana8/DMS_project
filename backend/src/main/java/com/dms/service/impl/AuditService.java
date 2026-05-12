package com.dms.service.impl;

import com.dms.dto.response.AuditLogResponse;
import com.dms.entity.AuditLog;
import com.dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repo;

    // ── Async log creation (never blocks HTTP thread) ─────────────────────
    @Async
    @Transactional
    public void log(String performedBy, String ip, AuditLog.Action action,
            String entityType, Long entityId, String description,
            String changeData, String endpoint, Integer statusCode) {
        try {
            repo.save(AuditLog.builder()
                    .performedBy(performedBy != null ? performedBy : "system")
                    .ipAddress(ip)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .changeData(changeData)
                    .endpoint(endpoint)
                    .statusCode(statusCode)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    // Convenience overload
    @Async
    @Transactional
    public void log(String performedBy, AuditLog.Action action, String description) {
        log(performedBy, null, action, null, null, description, null, null, 200);
    }

    // ── Query ─────────────────────────────────────────────────────────────
    public Page<AuditLogResponse> search(String user, String action, String entityType,
            LocalDateTime from, LocalDateTime to,
            int page, int size) {
        AuditLog.Action actionEnum = null;
        if (action != null && !action.isBlank()) {
            try {
                actionEnum = AuditLog.Action.valueOf(action);
            } catch (Exception ignored) {
            }
        }
        Pageable pg = PageRequest.of(page, size);
        return repo.search(
                user != null && !user.isBlank() ? user.toLowerCase() : null,
                actionEnum != null ? actionEnum.name() : null,
                entityType != null && !entityType.isBlank() ? entityType : null,
                from, to, pg
        ).map(this::toResponse);
    }

    public List<AuditLogResponse> getEntityHistory(String entityType, Long entityId) {
        return repo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toResponse).toList();
    }

    public Map<String, Object> getStats(LocalDateTime since) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Object[]> userCounts = repo.topUsersByActivity(since);
        List<Object[]> actionCounts = repo.actionCounts(since);
        stats.put("topUsers", userCounts.stream().limit(10)
                .map(r -> Map.of("user", r[0], "count", r[1])).toList());
        stats.put("actionCounts", actionCounts.stream()
                .map(r -> Map.of("action", r[0].toString(), "count", r[1])).toList());
        return stats;
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .performedBy(a.getPerformedBy())
                .ipAddress(a.getIpAddress())
                .action(a.getAction())
                .actionLabel(formatAction(a.getAction()))
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .description(a.getDescription())
                .changeData(a.getChangeData())
                .endpoint(a.getEndpoint())
                .statusCode(a.getStatusCode())
                .createdAt(a.getCreatedAt())
                .severity(getSeverity(a.getAction()))
                .build();
    }

    private String formatAction(AuditLog.Action a) {
        return a.name().replace('_', ' ').toLowerCase()
                .substring(0, 1).toUpperCase()
                + a.name().replace('_', ' ').toLowerCase().substring(1);
    }

    private String getSeverity(AuditLog.Action a) {
        return switch (a) {
            case USER_DEPRECATE, DOCUMENT_DEPRECATE, ROLE_CHANGE, PASSWORD_RESET, SETTINGS_CHANGE ->
                "WARNING";
            case USER_DEACTIVATE, DOCUMENT_DELETE ->
                "CRITICAL";
            default ->
                "INFO";
        };
    }
}
