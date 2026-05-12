package com.dms.controller;

import com.dms.dto.request.SystemSettingRequest;
import com.dms.dto.response.ApiResponse;
import com.dms.service.impl.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/settings") @RequiredArgsConstructor
public class SettingsController {

    private final SettingsService svc;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAll() {
        return ResponseEntity.ok(svc.getAll());
    }

    @GetMapping("/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable String category) {
        return ResponseEntity.ok(svc.getByCategory(category));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> update(
            @RequestBody SystemSettingRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(svc.updateBatch(req.getSettings(), ud.getUsername()));
    }
}
