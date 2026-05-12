package com.dms.controller;

import com.dms.dto.response.AuditLogResponse;
import com.dms.service.impl.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin-only audit trail endpoint.
 * GET /api/audit?user=&action=&entityType=&from=&to=&page=&size=
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.search(user, action, entityType, from, to, page, size));
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<List<AuditLogResponse>> entityHistory(
            @PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(auditService.getEntityHistory(type, id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since) {
        return ResponseEntity.ok(auditService.getStats(
                since != null ? since : LocalDateTime.now().minusDays(30)));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> actions() {
        return ResponseEntity.ok(Arrays.stream(
                com.dms.entity.AuditLog.Action.values())
                .map(Enum::name).toList());
    }
}
