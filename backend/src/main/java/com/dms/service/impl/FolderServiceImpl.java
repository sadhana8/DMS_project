package com.dms.service.impl;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.entity.*;
import com.dms.exception.*;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for {@link Folder} entities.
 *
 * <p><b>Ownership model:</b> every folder has exactly one owner. Only the
 * owner or an admin can mutate a folder. Public folders can be read by any
 * authenticated user; private folders are visible only to the owner and
 * admins.
 *
 * <p><b>Tree safety:</b> when a folder is moved, the service verifies the
 * move does not create a cycle (i.e. the new parent is not itself a
 * descendant). Folders cannot be deleted while they still contain documents
 * or subfolders — callers must cascade-delete children first, or use
 * {@link #deleteRecursive(Long, String)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class FolderServiceImpl {

    private final FolderRepository   folderRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository     userRepository;
    private final AuthServiceImpl    authService;
    private final AuditService       auditService;

    // ── Create ───────────────────────────────────────────────────────────
    @Transactional
    public FolderResponse create(CreateFolderRequest req, String userEmail) {
        User owner = getUser(userEmail);

        Folder parent = null;
        if (req.getParentId() != null) {
            parent = folderRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
            ensureOwnerOrAdmin(parent, owner);
        }

        if (folderRepository.existsSiblingWithName(
                owner, req.getName(),
                parent != null ? parent.getId() : null)) {
            throw new DuplicateResourceException(
                    "A folder named \"" + req.getName() + "\" already exists here");
        }

        Folder folder = Folder.builder()
                .name(req.getName())
                .description(req.getDescription())
                .parent(parent)
                .owner(owner)
                .isPublic(Boolean.TRUE.equals(req.getIsPublic()))
                .build();
        folder = folderRepository.save(folder);

        auditService.log(userEmail, AuditLog.Action.SYSTEM,
                "Created folder \"" + folder.getName() + "\" (id=" + folder.getId() + ")");
        return toResponse(folder, false);
    }

    // ── Read ─────────────────────────────────────────────────────────────
    public FolderResponse getById(Long id, String userEmail) {
        Folder folder = find(id);
        User user = getUser(userEmail);
        ensureReadable(folder, user);
        return toResponse(folder, false);
    }

    /** Root folders the user can see: their own, plus public ones. */
    public List<FolderResponse> listRoots(String userEmail) {
        User user = getUser(userEmail);
        Set<Folder> roots = new LinkedHashSet<>(folderRepository.findByOwnerAndParentIsNull(user));
        roots.addAll(folderRepository.findByIsPublicTrueAndParentIsNull());
        return roots.stream().map(f -> toResponse(f, false)).collect(Collectors.toList());
    }

    /** Direct children of a folder. */
    public List<FolderResponse> listChildren(Long parentId, String userEmail) {
        Folder parent = find(parentId);
        User user = getUser(userEmail);
        ensureReadable(parent, user);
        return folderRepository.findByParentId(parentId).stream()
                .map(f -> toResponse(f, false)).collect(Collectors.toList());
    }

    /**
     * The user's entire folder tree, fully expanded. Intended for small
     * trees (tens to low hundreds of nodes). For huge trees, prefer lazy
     * child-loading via {@link #listChildren(Long, String)}.
     */
    public List<FolderResponse> getTree(String userEmail) {
        User user = getUser(userEmail);
        List<Folder> all = folderRepository.findByOwnerOrderByNameAsc(user);
        Map<Long, FolderResponse> byId = new LinkedHashMap<>();
        for (Folder f : all) byId.put(f.getId(), toResponse(f, true));

        List<FolderResponse> roots = new ArrayList<>();
        for (Folder f : all) {
            FolderResponse node = byId.get(f.getId());
            if (f.getParent() == null) {
                roots.add(node);
            } else {
                FolderResponse parent = byId.get(f.getParent().getId());
                if (parent != null) parent.getChildren().add(node);
                else roots.add(node);  // parent not owned — surface as root
            }
        }
        return roots;
    }

    // ── Update ───────────────────────────────────────────────────────────
    @Transactional
    public FolderResponse update(Long id, UpdateFolderRequest req, String userEmail) {
        Folder folder = find(id);
        User user = getUser(userEmail);
        ensureOwnerOrAdmin(folder, user);

        if (req.getName() != null) {
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
            if (!req.getName().equalsIgnoreCase(folder.getName())
                    && folderRepository.existsSiblingWithName(folder.getOwner(), req.getName(), parentId)) {
                throw new DuplicateResourceException(
                        "A folder named \"" + req.getName() + "\" already exists here");
            }
            folder.setName(req.getName());
        }
        if (req.getDescription() != null) folder.setDescription(req.getDescription());
        if (req.getIsPublic()    != null) folder.setIsPublic(req.getIsPublic());

        if (req.getParentId() != null) {
            if (req.getParentId() < 0) {
                folder.setParent(null);
            } else if (!req.getParentId().equals(folder.getParent() != null ? folder.getParent().getId() : null)) {
                Folder newParent = folderRepository.findById(req.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Target parent not found"));
                ensureOwnerOrAdmin(newParent, user);
                if (isDescendant(newParent, folder)) {
                    throw new IllegalArgumentException(
                            "Cannot move folder into one of its own descendants");
                }
                folder.setParent(newParent);
            }
        }

        auditService.log(userEmail, AuditLog.Action.SYSTEM,
                "Updated folder \"" + folder.getName() + "\" (id=" + folder.getId() + ")");
        return toResponse(folderRepository.save(folder), false);
    }

    // ── Delete ───────────────────────────────────────────────────────────
    /** Deletes an empty folder. Throws if it has children or documents. */
    @Transactional
    public void delete(Long id, String userEmail) {
        Folder folder = find(id);
        User user = getUser(userEmail);
        ensureOwnerOrAdmin(folder, user);

        if (!folderRepository.findByParentId(id).isEmpty()) {
            throw new IllegalStateException("Folder is not empty — it still contains subfolders");
        }
        if (!folder.getDocuments().isEmpty()) {
            throw new IllegalStateException(
                    "Folder still contains documents — move or deprecate them first");
        }
        folderRepository.delete(folder);
        auditService.log(userEmail, AuditLog.Action.SYSTEM,
                "Deleted folder (id=" + id + ")");
    }

    /**
     * Recursive delete. Walks the whole subtree, detaches all documents
     * (sets their {@code folder} reference to {@code null}) and removes every
     * folder. Use with care.
     */
    @Transactional
    public void deleteRecursive(Long id, String userEmail) {
        Folder folder = find(id);
        User user = getUser(userEmail);
        ensureOwnerOrAdmin(folder, user);
        deleteSubtree(folder);
        auditService.log(userEmail, AuditLog.Action.SYSTEM,
                "Recursively deleted folder (id=" + id + ")");
    }

    private void deleteSubtree(Folder folder) {
        for (Folder child : new ArrayList<>(folderRepository.findByParentId(folder.getId()))) {
            deleteSubtree(child);
        }
        folder.getDocuments().forEach(d -> d.setFolder(null));
        folderRepository.delete(folder);
    }

    // ── Access helpers ───────────────────────────────────────────────────
    private void ensureReadable(Folder folder, User user) {
        if (isAdmin(user) || folder.getOwner().getId().equals(user.getId())
                || Boolean.TRUE.equals(folder.getIsPublic())) return;
        throw new AccessDeniedException("You don't have access to this folder");
    }

    private void ensureOwnerOrAdmin(Folder folder, User user) {
        if (isAdmin(user) || folder.getOwner().getId().equals(user.getId())) return;
        throw new AccessDeniedException("Only the owner or an admin can modify this folder");
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
    }

    /** Returns true if {@code candidate} is the same as {@code ancestor} or lives under it. */
    private boolean isDescendant(Folder candidate, Folder ancestor) {
        Folder cursor = candidate;
        while (cursor != null) {
            if (cursor.getId().equals(ancestor.getId())) return true;
            cursor = cursor.getParent();
        }
        return false;
    }

    private Folder find(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found: " + id));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    // ── Mapper ───────────────────────────────────────────────────────────
    private FolderResponse toResponse(Folder f, boolean includeChildrenList) {
        int docCount = (int) documentRepository.countByFolderId(f.getId());
        return FolderResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .description(f.getDescription())
                .parentId(f.getParent() != null ? f.getParent().getId() : null)
                .parentName(f.getParent() != null ? f.getParent().getName() : null)
                .owner(authService.mapUserToResponse(f.getOwner()))
                .isPublic(Boolean.TRUE.equals(f.getIsPublic()))
                .documentCount(docCount)
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .children(includeChildrenList ? new ArrayList<>() : null)
                .build();
    }
}
