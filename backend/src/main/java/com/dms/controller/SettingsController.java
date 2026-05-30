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

    /**
     * Public endpoint — returns company branding & app name so the login page
     * and sidebar can display the correct company name/logo without authentication.
     */
    @GetMapping("/public/company")
    public ResponseEntity<Map<String, String>> getPublicCompanyInfo() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put("company_name",    svc.get("company_name")    != null ? svc.get("company_name")    : "DocVault");
        info.put("company_logo_url", svc.get("company_logo_url") != null ? svc.get("company_logo_url") : "");
        info.put("app_name",        svc.get("app_name")        != null ? svc.get("app_name")        : "DocVault");
        info.put("app_version",     svc.get("app_version")     != null ? svc.get("app_version")     : "1.0.0");
        info.put("company_email",   svc.get("company_email")   != null ? svc.get("company_email")   : "");
        info.put("company_phone",   svc.get("company_phone")   != null ? svc.get("company_phone")   : "");
        info.put("company_website", svc.get("company_website") != null ? svc.get("company_website") : "");
        return ResponseEntity.ok(info);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> update(
            @RequestBody SystemSettingRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(svc.updateBatch(req.getSettings(), ud.getUsername()));
    }
}
