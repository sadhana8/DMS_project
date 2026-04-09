package com.dms.repository;

import com.dms.entity.Folder;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Folder} entities.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    /**
     * Returns all root-level folders (i.e. those with no parent) owned by a
     * user. Used to render the top-level of the folder tree in the UI.
     *
     * @param owner the owning {@link User}
     * @return list of root folders belonging to the owner
     */
    List<Folder> findByOwnerAndParentIsNull(User owner);

    /**
     * Returns all direct child folders of a given parent folder. Used to lazily
     * expand a node in the folder tree.
     *
     * @param parentId the ID of the parent folder
     * @return list of immediate sub-folders
     */
    List<Folder> findByParentId(Long parentId);
}
