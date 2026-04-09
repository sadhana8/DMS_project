package com.dms.repository;

import com.dms.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DocumentVersion} entities.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * Returns the full version history for a document, newest version first.
     *
     * @param documentId the ID of the parent document
     * @return list of versions ordered by {@code versionNumber} descending
     */
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);

    /**
     * Finds a specific version of a document by its version number.
     *
     * @param documentId the ID of the parent document
     * @param versionNumber the one-based version number
     * @return an {@link Optional} containing the version, or empty if not found
     */
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, Integer versionNumber);

    /**
     * Returns how many versions a document currently has.
     *
     * @param documentId the ID of the document
     * @return the total number of version records for that document
     */
    long countByDocumentId(Long documentId);
}
