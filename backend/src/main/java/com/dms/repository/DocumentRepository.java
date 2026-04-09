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

/**
 * Spring Data JPA repository for {@link Document} entities.
 *
 * <p>
 * All standard queries filter on {@code deprecation_status = 'ACTIVE'}.
 * Deprecated documents are excluded from normal results and only appear through
 * the admin deprecated-documents endpoints.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Returns all non-deprecated documents the user is permitted to access.
     *
     * @param user the requesting {@link User}
     * @param pageable pagination and sorting
     * @return a page of accessible, active documents
     */
    @Query("SELECT d FROM Document d WHERE d.deprecationStatus = 'ACTIVE' AND "
            + "(d.owner = :user OR d.isPublic = true OR "
            + "EXISTS (SELECT p FROM DocumentPermission p WHERE p.document = d AND p.user = :user))")
    Page<Document> findAccessibleByUser(@Param("user") User user, Pageable pageable);

    /**
     * Full-text search restricted to non-deprecated, accessible documents.
     *
     * @param query search term (partial, case-insensitive)
     * @param user the requesting user
     * @param pageable pagination and sorting
     * @return matching accessible documents
     */
    @Query("SELECT d FROM Document d WHERE d.deprecationStatus = 'ACTIVE' AND "
            + "(d.owner = :user OR d.isPublic = true OR "
            + "EXISTS (SELECT p FROM DocumentPermission p WHERE p.document = d AND p.user = :user)) AND "
            + "(LOWER(d.title)       LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + " LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + " LOWER(d.tags)        LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("query") String query,
            @Param("user") User user,
            Pageable pageable);

    /**
     * Returns all non-deprecated documents (admin view).
     *
     * @param pageable pagination and sorting
     * @return all active documents
     */
    @Query("SELECT d FROM Document d WHERE d.deprecationStatus = 'ACTIVE'")
    Page<Document> findAllActive(Pageable pageable);

    /**
     * Returns all deprecated documents (admin view).
     *
     * @param pageable pagination and sorting
     * @return deprecated documents ordered by deprecation date descending
     */
    @Query("SELECT d FROM Document d WHERE d.deprecationStatus = 'DEPRECATED' ORDER BY d.deprecatedAt DESC")
    Page<Document> findAllDeprecated(Pageable pageable);

    /**
     * Count of non-deprecated documents.
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.deprecationStatus = 'ACTIVE'")
    long countActiveDocuments();

    /**
     * Sum of file sizes for non-deprecated documents.
     */
    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.deprecationStatus = 'ACTIVE'")
    Long getTotalStorageUsed();

    /**
     * Most recent non-deprecated documents, newest first.
     */
    @Query("SELECT d FROM Document d WHERE d.deprecationStatus = 'ACTIVE' ORDER BY d.createdAt DESC")
    List<Document> findRecentDocuments(Pageable pageable);
}
