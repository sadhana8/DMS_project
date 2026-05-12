package com.dms.repository;

import com.dms.entity.Document;
import com.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByOwner(User owner, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED'")
    Page<Document> findActiveByOwner(@Param("owner") User owner, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
           "(d.owner = :user OR d.isPublic = true OR " +
           "EXISTS (SELECT p FROM DocumentPermission p WHERE p.document = d AND p.user = :user)) " +
           "AND d.status != 'DELETED'")
    Page<Document> findAccessibleByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
           "(d.owner = :user OR d.isPublic = true OR " +
           "EXISTS (SELECT p FROM DocumentPermission p WHERE p.document = d AND p.user = :user)) " +
           "AND d.status != 'DELETED' AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("query") String query, @Param("user") User user, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.status != 'DELETED'")
    Page<Document> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status <> 'DELETED' AND d.status <> 'ARCHIVED'")
    long countActiveDocuments();

    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.status <> 'DELETED' AND d.status <> 'ARCHIVED'")
    Long getTotalStorageUsed();

    /** Count documents (non-deleted, non-archived) inside a folder. */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.folder.id = :folderId " +
           "AND d.status <> 'DELETED' AND d.status <> 'ARCHIVED'")
    long countByFolderId(@Param("folderId") Long folderId);

    @Query("SELECT d FROM Document d WHERE d.status != 'DELETED' AND d.status != 'ARCHIVED' ORDER BY d.createdAt DESC")
    List<Document> findRecentDocuments(Pageable pageable);

    /** Documents inside a given folder (excluding deleted/archived). */
    @Query("SELECT d FROM Document d WHERE d.folder.id = :folderId " +
           "AND d.status <> 'DELETED' AND d.status <> 'ARCHIVED'")
    Page<Document> findByFolderId(@Param("folderId") Long folderId, Pageable pageable);

    /** Documents that have been soft-deprecated (ARCHIVED) — admin listing. */
    @Query("SELECT d FROM Document d WHERE d.status = 'ARCHIVED' ORDER BY d.updatedAt DESC")
    Page<Document> findDeprecated(Pageable pageable);

    /** Count of documents created since the given timestamp. */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.status <> 'DELETED' AND d.createdAt >= :since")
    long countCreatedSince(@Param("since") java.time.LocalDateTime since);

    /** Count of documents owned by a specific user (excluding deleted). */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.owner = :owner AND d.status <> 'DELETED'")
    long countByOwner(@Param("owner") User owner);

    /** Storage bytes used by a specific user. */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.owner = :owner AND d.status <> 'DELETED'")
    long getStorageUsedBy(@Param("owner") User owner);

    /** Group documents by extension for the storage-breakdown dashboard widget. */
    @Query("SELECT COALESCE(d.fileType, 'OTHER') AS type, COUNT(d), COALESCE(SUM(d.fileSize), 0) " +
           "FROM Document d WHERE d.status <> 'DELETED' GROUP BY d.fileType")
    List<Object[]> groupByFileType();
}
