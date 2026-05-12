package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Immutable audit log entry. Every write action in the system creates one row.
 * Never deleted — provides the full audit trail accessible by admins.
 */
@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user",      columnList = "performed_by"),
        @Index(name = "idx_audit_entity",    columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_action",    columnList = "action"),
        @Index(name = "idx_audit_created",   columnList = "created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who did the action — username string kept even if user is later deprecated. */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    /** IP address of the request, for security investigations. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Category of action performed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private Action action;

    /** The type of entity affected (USER, DOCUMENT, ROLE, etc.). */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** The ID of the affected entity (null for system-level events). */
    @Column(name = "entity_id")
    private Long entityId;

    /** Short description shown in the audit table. */
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** JSON snapshot of the changed fields: {"before": {...}, "after": {...}}. */
    @Column(name = "change_data", columnDefinition = "TEXT")
    private String changeData;

    /** HTTP method + path (e.g. "PUT /api/documents/42"). */
    @Column(name = "endpoint", length = 200)
    private String endpoint;

    /** HTTP response status code. */
    @Column(name = "status_code")
    private Integer statusCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Action {
        // Auth
        LOGIN, LOGOUT, REGISTER, PASSWORD_CHANGE, PASSWORD_RESET,
        // Document
        DOCUMENT_CREATE, DOCUMENT_UPDATE, DOCUMENT_DELETE, DOCUMENT_DOWNLOAD,
        DOCUMENT_SHARE, DOCUMENT_DEPRECATE, DOCUMENT_RESTORE, VERSION_UPLOAD,
        // User
        USER_CREATE, USER_UPDATE, USER_DEPRECATE, USER_RESTORE,
        USER_ACTIVATE, USER_DEACTIVATE, ROLE_CHANGE,
        USER_TERMINATE, USER_SUSPEND, USER_RESIGN, USER_ACCESS_REVOKED,
        // Admin
        SETTINGS_CHANGE, SYSTEM
    }
}
