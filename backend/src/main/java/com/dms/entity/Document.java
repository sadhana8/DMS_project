package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA entity representing a document stored in the DocVault system.
 *
 * <p>
 * Maps to the {@code documents} table. The binary file lives on disk; this
 * entity stores only the metadata and lifecycle state.
 *
 * <h2>Soft deprecation — no hard deletes</h2>
 * Documents are <b>never physically deleted</b>. "Deleting" a document sets
 * {@link #deprecationStatus} to {@link DeprecationStatus#DEPRECATED}. The file
 * bytes on disk are retained. An admin can restore the document at any time via
 * {@code PUT /admin/deprecated/documents/{id}/restore}.
 *
 * <h2>Status vs. deprecation</h2> {@link #status} tracks the editorial
 * lifecycle (ACTIVE → ARCHIVED → PENDING_REVIEW). {@link #deprecationStatus}
 * tracks the visibility/access lifecycle (ACTIVE → DEPRECATED →
 * PERMANENTLY_DELETED). Both are independent fields.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see DeprecationStatus
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * UUID-based stored file name — never the client-supplied name.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Original file name as supplied by the client at upload time.
     */
    @Column(name = "original_file_name")
    private String originalFileName;

    /**
     * Relative path from the storage root to the current version's file.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "current_version")
    @Builder.Default
    private Integer currentVersion = 1;

    /**
     * Editorial lifecycle status (ACTIVE, ARCHIVED, PENDING_REVIEW).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    /**
     * Soft-deprecation lifecycle state. Set to
     * {@link DeprecationStatus#DEPRECATED} instead of deleting the record.
     * Deprecated documents are excluded from all standard search and list
     * queries.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deprecation_status", nullable = false)
    @Builder.Default
    private DeprecationStatus deprecationStatus = DeprecationStatus.ACTIVE;

    /**
     * Timestamp when this document was deprecated; {@code null} when active.
     */
    @Column(name = "deprecated_at")
    private LocalDateTime deprecatedAt;

    /**
     * Reason provided by the user or admin who deprecated this document.
     */
    @Column(name = "deprecation_reason", columnDefinition = "TEXT")
    private String deprecationReason;

    /**
     * Username of the person who deprecated this document. Stored as a plain
     * string to preserve the audit trail.
     */
    @Column(name = "deprecated_by")
    private String deprecatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DocumentPermission> permissions = new HashSet<>();

    @Column(name = "tags")
    private String tags;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "download_count")
    @Builder.Default
    private Long downloadCount = 0L;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Returns {@code true} if this document has been deprecated or permanently
     * deleted.
     *
     * @return {@code true} when not in {@link DeprecationStatus#ACTIVE} state
     */
    public boolean isDeprecated() {
        return deprecationStatus != DeprecationStatus.ACTIVE;
    }

    /**
     * Editorial lifecycle states independent of the deprecation state.
     */
    public enum DocumentStatus {
        ACTIVE, ARCHIVED, PENDING_REVIEW
    }
}
