package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a folder used to organise {@link Document} records.
 *
 * <p>
 * Maps to the {@code folders} table. Supports unlimited nesting through a
 * self-referential {@link #parent} / {@link #subFolders} relationship.
 * Root-level folders have a {@code null} parent.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    /**
     * Auto-generated surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the folder (e.g. {@code "Finance / Q3 2024"}).
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional description of the folder's purpose.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The parent folder. {@code null} for root-level folders. Cascading is
     * intentionally omitted so that deleting a parent folder does not silently
     * delete all children; the service layer must handle this explicitly.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    /**
     * Direct child folders of this folder.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Folder> subFolders = new ArrayList<>();

    /**
     * Documents directly inside this folder (not in sub-folders).
     */
    @OneToMany(mappedBy = "folder")
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    /**
     * The user who created and owns this folder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * When {@code true}, any authenticated user can see the folder and its
     * contents.
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Timestamp when the folder was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to this folder's metadata.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
