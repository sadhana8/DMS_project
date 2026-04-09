package com.dms.repository;

import com.dms.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DocumentPermission} entities.
 *
 * <p>
 * Used to manage who can access which document and at what permission level.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    /**
     * Returns all permission entries for a given document (the full ACL).
     *
     * @param documentId the ID of the document
     * @return list of all permissions granted for that document
     */
    List<DocumentPermission> findByDocumentId(Long documentId);

    /**
     * Finds a single permission entry for a specific user on a specific
     * document.
     *
     * @param documentId the ID of the document
     * @param userId the ID of the user
     * @return an {@link Optional} containing the permission, or empty if no
     * access was granted
     */
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);

    /**
     * Removes a user's access to a document entirely.
     *
     * @param documentId the ID of the document
     * @param userId the ID of the user whose access should be revoked
     */
    void deleteByDocumentIdAndUserId(Long documentId, Long userId);

    /**
     * Checks whether a user has any permission entry for a document (used for
     * fast access-control checks without loading the full entity).
     *
     * @param documentId the ID of the document
     * @param userId the ID of the user
     * @return {@code true} if any permission record exists for this
     * user/document pair
     */
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);
}
