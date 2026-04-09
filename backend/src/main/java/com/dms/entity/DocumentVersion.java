package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a single version snapshot of a {@link Document}.
 *
 * <p>
 * Maps to the {@code document_versions} table. Every time a new file is
 * uploaded for an existing document a new {@code DocumentVersion} is created,
 * allowing users to view the full revision history, download any past version,
 * and restore a previous version as the current one.
 *
 * <p>
 * The first version (number {@code 1}) is created automatically when the
 * document is uploaded. Subsequent versions are created via
 * {@link com.dms.service.impl.DocumentServiceImpl#uploadNewVersion}.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Document
 */
@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent document this version belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * One-based version number (1 = initial upload, 2 = first revision, etc.).
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /**
     * UUID-based stored file name for this version.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Relative path from the storage root to this version's file.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * File size of this version in bytes.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Optional human-readable description of what changed in this version,
     * entered by the uploader at upload time.
     */
    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    /**
     * The user who uploaded this version.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    /**
     * Timestamp when this version was uploaded. Immutable after creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
