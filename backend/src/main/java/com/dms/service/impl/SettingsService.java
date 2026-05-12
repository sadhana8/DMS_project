package com.dms.service.impl;

import com.dms.entity.AuditLog;
import com.dms.entity.SystemSetting;
import com.dms.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemSettingRepository repo;
    private final AuditService auditService;

    // Default settings seeded into DB if missing
    private static final Map<String, String[]> DEFAULTS = new LinkedHashMap<>() {
        {
            put("require_email_verification", new String[]{"false", "Require users to verify email before login", "auth"});
            put("require_admin_approval", new String[]{"false", "New users must be approved by admin before login", "auth"});
            put("auto_approve_domains", new String[]{"", "Comma-separated email domains that bypass approval (e.g. acme.com,partner.com). Leave blank to disable.", "auth"});
            put("max_upload_size_mb", new String[]{"50", "Maximum file upload size in megabytes", "storage"});
            put("allowed_file_types", new String[]{"*", "Comma-separated file extensions, or * for all", "storage"});
            put("document_retention_days", new String[]{"365", "Days to keep deprecated documents before permanent deletion", "storage"});
            put("max_versions_per_document", new String[]{"20", "Maximum number of versions kept per document", "storage"});
            put("session_timeout_minutes", new String[]{"1440", "JWT access token lifetime in minutes", "security"});
            put("allow_public_documents", new String[]{"true", "Allow users to mark documents as public", "documents"});
            put("allow_document_sharing", new String[]{"true", "Allow users to share documents with others", "documents"});
            put("notify_on_document_share", new String[]{"true", "Send notification when document is shared", "notifications"});
            put("notify_on_version_upload", new String[]{"true", "Send notification on new version upload", "notifications"});
            put("smtp_enabled", new String[]{"true", "Enable outgoing email via SMTP", "email"});
            put("maintenance_mode", new String[]{"false", "Put system in read-only maintenance mode", "system"});
            put("app_name", new String[]{"DocVault", "Application name shown in UI and emails", "system"});
        }
    };

    public Map<String, Object> getAll() {
        ensureDefaults();
        List<SystemSetting> all = repo.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemSetting s : all) {
            result.put(s.getKey(), Map.of(
                    "value", s.getValue(),
                    "description", s.getDescription() != null ? s.getDescription() : "",
                    "category", s.getCategory() != null ? s.getCategory() : "general"
            ));
        }
        return result;
    }

    public Map<String, Object> getByCategory(String category) {
        List<SystemSetting> settings = repo.findByCategory(category);
        Map<String, Object> result = new LinkedHashMap<>();
        settings.forEach(s -> result.put(s.getKey(), Map.of(
                "value", s.getValue(),
                "description", s.getDescription() != null ? s.getDescription() : "")));
        return result;
    }

    public String get(String key) {
        return repo.findById(key).map(SystemSetting::getValue)
                .orElseGet(() -> {
                    String[] def = DEFAULTS.get(key);
                    return def != null ? def[0] : null;
                });
    }

    public boolean getBool(String key) {
        return "true".equalsIgnoreCase(get(key));
    }

    @Transactional
    public Map<String, Object> updateBatch(Map<String, String> updates, String adminEmail) {
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, String> e : updates.entrySet()) {
            SystemSetting s = repo.findById(e.getKey()).orElse(
                    SystemSetting.builder().key(e.getKey()).build());
            s.setValue(e.getValue());
            s.setUpdatedBy(adminEmail);
            if (DEFAULTS.containsKey(e.getKey())) {
                s.setDescription(DEFAULTS.get(e.getKey())[1]);
                s.setCategory(DEFAULTS.get(e.getKey())[2]);
            }
            repo.save(s);
            changed.add(e.getKey() + "=" + e.getValue());
        }
        auditService.log(adminEmail, AuditLog.Action.SETTINGS_CHANGE,
                "Updated settings: " + String.join(", ", changed));
        return getAll();
    }

    @Transactional
    public void ensureDefaults() {
        DEFAULTS.forEach((key, vals) -> {
            if (!repo.existsById(key)) {
                repo.save(SystemSetting.builder()
                        .key(key).value(vals[0])
                        .description(vals[1]).category(vals[2]).build());
            }
        });
    }
}
