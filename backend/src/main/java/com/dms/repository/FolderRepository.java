package com.dms.repository;

import com.dms.entity.Folder;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Folder}. Exposes the queries needed by the folder
 * controller for tree browsing, sibling lookup, and name-uniqueness checks.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    /** All root folders (no parent) for a given owner. */
    List<Folder> findByOwnerAndParentIsNull(User owner);

    /** All direct children of a folder. */
    List<Folder> findByParentId(Long parentId);

    /** All folders owned by a user, ordered by name. */
    List<Folder> findByOwnerOrderByNameAsc(User owner);

    /** Root folders visible to anyone (public at the top level). */
    List<Folder> findByIsPublicTrueAndParentIsNull();

    /**
     * Whether a folder with the given name already exists under the given
     * parent (or at root if parentId is null) for this owner.
     */
    @Query("SELECT COUNT(f) > 0 FROM Folder f WHERE f.owner = :owner " +
           "AND LOWER(f.name) = LOWER(:name) " +
           "AND ((:parentId IS NULL AND f.parent IS NULL) OR f.parent.id = :parentId)")
    boolean existsSiblingWithName(@Param("owner") User owner,
                                  @Param("name") String name,
                                  @Param("parentId") Long parentId);

    /** Finds a folder by id but only if the user owns it. */
    @Query("SELECT f FROM Folder f WHERE f.id = :id AND f.owner = :owner")
    Optional<Folder> findOwnedById(@Param("id") Long id, @Param("owner") User owner);
}
