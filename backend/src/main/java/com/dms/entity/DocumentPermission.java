package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing the access a specific user has been granted to a
 * specific {@link Document}.
 *
 * <p>
 * Maps to the {@code document_permissions} table. A composite unique constraint
 * on {@code (document_id, user_id)} ensures each user has at most one
 * permission entry per document; to change the level the existing row is
 * updated rather than inserting a duplicate.
 *
 * <h2>Permission levels (ordered by capability)</h2>
 * <ol>
 * <li>{@link PermissionType#VIEW} – can open and read the document.</li>
 * <li>{@link PermissionType#DOWNLOAD} – can also download the file.</li>
 * <li>{@link PermissionType#EDIT} – can also edit metadata and upload new
 * versions.</li>
 * <li>{@link PermissionType#ADMIN} – full control including sharing and
 * revoking access.</li>
 * </ol>
 *
 * <p>
 * Document owners and system admins always bypass this table.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Document
 * @see User
 */
@Entity
@Table(name = "document_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentPermission {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The document this permission entry refers to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * The user who has been granted access.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The level of access granted. Defaults to {@link PermissionType#VIEW} if
     * not specified when the permission is created.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PermissionType permission = PermissionType.VIEW;

    /**
     * Optional expiry date/time after which the permission is no longer valid.
     * {@code null} means the permission never expires.
     * <em>Expiry enforcement must be implemented in the service layer.</em>
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Timestamp when this permission was first created.
     */
    @CreationTimestamp
    @Column(name = "granted_at", updatable = false)
    private LocalDateTime grantedAt;

    /**
     * The user who granted this permission (document owner or admin).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    /**
     * Defines the available permission levels for document access control.
     */
    public enum PermissionType {
        /**
         * Can view the document in the browser preview.
         */
        VIEW,
        /**
         * Can view and download the document file.
         */
        DOWNLOAD,
        /**
         * Can view, download, edit metadata, and upload new versions.
         */
        EDIT,
        /**
         * Full control: all EDIT rights plus sharing and revoking access.
         */
        ADMIN
    }
}
