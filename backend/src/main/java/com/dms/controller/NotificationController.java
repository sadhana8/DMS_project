package com.dms.controller;

import com.dms.dto.request.UpdateNotificationSettingRequest;
import com.dms.dto.response.*;
import com.dms.service.impl.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/notifications") @RequiredArgsConstructor
public class NotificationController {

    private final NotificationService svc;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(svc.getFiltered(ud.getUsername(), type, isRead, from, to, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(Map.of("count", svc.getUnreadCount(ud.getUsername())));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        svc.markAllRead(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All marked as read"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markOneRead(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails ud) {
        svc.markOneRead(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Marked as read"));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<NotificationSettingResponse>> settings(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(svc.getSettings(ud.getUsername()));
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingResponse> updateSetting(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody UpdateNotificationSettingRequest req) {
        return ResponseEntity.ok(svc.updateSetting(ud.getUsername(), req));
    }

    @PostMapping("/settings/reset")
    public ResponseEntity<ApiResponse> resetSettings(@AuthenticationPrincipal UserDetails ud) {
        svc.resetSettings(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Settings reset to defaults"));
    }
}
