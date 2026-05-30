package com.dms.service.impl;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.entity.*;
import com.dms.exception.*;
import com.dms.repository.*;
import com.dms.service.EmailService;
import com.dms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

/**
 * Business logic for the {@link Document} lifecycle.
 *
 * <p><b>Lifecycle states</b> (see {@link Document.DocumentStatus}):
 * <ul>
 *   <li>{@code ACTIVE} — fully visible; the normal state.</li>
 *   <li>{@code ARCHIVED} — soft-deprecated. Hidden from normal listings, but
 *       the file is still on disk and an admin can restore it.</li>
 *   <li>{@code DELETED} — legacy; treated as {@code ARCHIVED} for visibility.</li>
 *   <li>{@code PENDING_REVIEW} — reserved for future moderation workflows.</li>
 * </ul>
 *
 * <p>"Delete" via the public API is a <b>soft</b> operation: the document
 * transitions to {@code ARCHIVED} rather than being removed. Only
 * {@link #purge(Long, String)} physically deletes a document from the DB and
 * its file from disk — that endpoint is admin-only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class DocumentServiceImpl {

    private final DocumentRepository            documentRepository;
    private final DocumentVersionRepository     versionRepository;
    private final DocumentPermissionRepository  permissionRepository;
    private final FolderRepository              folderRepository;
    private final UserRepository                userRepository;
    private final FileStorageService            fileStorageService;
    private final EmailService                  emailService;
    private final AuthServiceImpl               authService;
    private final AuditService                  auditService;
    private final Tika                          tika = new Tika();

    @Value("${app.storage.max-file-size:5242880}")
    private long maxFileSizeBytes;

    // ── List / Search ────────────────────────────────────────────────────
    public Page<DocumentResponse> listDocuments(String userEmail, int page, int size, String status) {
        User user   = getUser(userEmail);
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // `status` is kept for compatibility but currently ignored by the query;
        // deprecated/archived/deleted documents are excluded at the repo level.
        return documentRepository.findAccessibleByUser(user, pg).map(this::toResponse);
    }

    public Page<DocumentResponse> searchDocuments(String userEmail, String query, int page, int size) {
        User user   = getUser(userEmail);
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.searchDocuments(query, user, pg).map(this::toResponse);
    }

    public Page<DocumentResponse> listAllDocuments(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.findAllActive(pg).map(this::toResponse);
    }

    /** Documents inside a folder. Caller must have read access to the folder. */
    public Page<DocumentResponse> listInFolder(Long folderId, String userEmail, int page, int size) {
        User user = getUser(userEmail);
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!isAdmin(user)
                && !folder.getOwner().getId().equals(user.getId())
                && !Boolean.TRUE.equals(folder.getIsPublic())) {
            throw new AccessDeniedException("You don't have access to this folder");
        }
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.findByFolderId(folderId, pg).map(this::toResponse);
    }

    /** Admin-only: list all deprecated (ARCHIVED) documents. */
    public Page<DocumentResponse> listDeprecated(int page, int size) {
        Pageable pg = PageRequest.of(page, size);
        return documentRepository.findDeprecated(pg).map(this::toResponse);
    }

    // ── Get single ───────────────────────────────────────────────────────
    @Transactional
    public DocumentResponse getDocument(Long id, String userEmail) {
        Document doc  = findDoc(id);
        User     user = getUser(userEmail);
        checkReadAccess(doc, user);
        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);
        return toResponse(doc);
    }

    // ── Upload ───────────────────────────────────────────────────────────
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title, String description,
                                           String purpose, String tags, Boolean isPublic, Long folderId,
                                           String userEmail) {
        // Enforce 5 MB limit server-side (frontend already validates, but always check here too)
        if (file.getSize() > maxFileSizeBytes) {
            long mb = maxFileSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException("File size exceeds the " + mb + " MB limit");
        }
        User owner = getUser(userEmail);
        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
            if (!isAdmin(owner) && !folder.getOwner().getId().equals(owner.getId())) {
                throw new AccessDeniedException("You don't own the target folder");
            }
        }

        String mimeType  = detectMime(file);
        String filePath  = fileStorageService.storeWithUsername(file, owner.getUsername());
        long   fileSize  = file.getSize();
        String origName  = file.getOriginalFilename();
        String ext       = origName != null && origName.contains(".")
                           ? origName.substring(origName.lastIndexOf(".") + 1).toUpperCase() : "FILE";

        Document doc = Document.builder()
                .title(title != null && !title.isBlank() ? title : origName)
                .description(description)
                .uploadPurpose(purpose)
                .fileName(filePath.substring(filePath.lastIndexOf("/") + 1))
                .originalFileName(origName)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileType(ext)
                .mimeType(mimeType)
                .owner(owner)
                .folder(folder)
                .tags(tags)
                .isPublic(Boolean.TRUE.equals(isPublic))
                .currentVersion(1)
                .build();
        documentRepository.save(doc);

        // Save initial version
        DocumentVersion v = DocumentVersion.builder()
                .document(doc)
                .versionNumber(1)
                .fileName(doc.getFileName())
                .filePath(filePath)
                .fileSize(fileSize)
                .changeSummary("Initial upload")
                .uploadedBy(owner)
                .build();
        versionRepository.save(v);

        auditService.log(userEmail, null, AuditLog.Action.DOCUMENT_CREATE,
                "DOCUMENT", doc.getId(),
                "Uploaded \"" + doc.getTitle() + "\"", null, null, 201);
        log.info("Document uploaded: {} by {}", doc.getTitle(), userEmail);
        return toResponse(doc);
    }

    // ── Update metadata ──────────────────────────────────────────────────
    @Transactional
    public DocumentResponse updateDocument(Long id, UpdateDocumentRequest req, String userEmail) {
        Document doc  = findDoc(id);
        User     user = getUser(userEmail);
        checkEditAccess(doc, user);

        if (req.getTitle()       != null) doc.setTitle(req.getTitle());
        if (req.getDescription() != null) doc.setDescription(req.getDescription());
        if (req.getTags()        != null) doc.setTags(req.getTags());
        if (req.getIsPublic()    != null) doc.setIsPublic(req.getIsPublic());
        if (req.getStatus()      != null) {
            try { doc.setStatus(Document.DocumentStatus.valueOf(req.getStatus())); }
            catch (IllegalArgumentException ignored) { /* reject silently */ }
        }
        auditService.log(userEmail, AuditLog.Action.DOCUMENT_UPDATE,
                "Updated \"" + doc.getTitle() + "\"");
        return toResponse(documentRepository.save(doc));
    }

    /** Move a document into another folder (or to no folder when {@code folderId == null}). */
    @Transactional
    public DocumentResponse moveToFolder(Long docId, Long folderId, String userEmail) {
        Document doc = findDoc(docId);
        User user = getUser(userEmail);
        checkEditAccess(doc, user);

        if (folderId == null) {
            doc.setFolder(null);
        } else {
            Folder target = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target folder not found"));
            if (!isAdmin(user) && !target.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You don't own the target folder");
            }
            doc.setFolder(target);
        }
        return toResponse(documentRepository.save(doc));
    }

    // ── Soft-deprecate / Restore / Purge ─────────────────────────────────
    /** Soft-deprecate: keep the row, hide from listings. Reversible. */
    @Transactional
    public DocumentResponse deprecate(Long id, String reason, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (!doc.getOwner().getEmail().equals(userEmail) && !isAdmin(user))
            throw new AccessDeniedException("Only the owner or an admin can deprecate this document");
        doc.setStatus(Document.DocumentStatus.ARCHIVED);
        Document saved = documentRepository.save(doc);
        auditService.log(userEmail, null, AuditLog.Action.DOCUMENT_DEPRECATE,
                "DOCUMENT", id,
                "Deprecated \"" + doc.getTitle() + "\"" + (reason != null ? " — " + reason : ""),
                null, null, 200);
        return toResponse(saved);
    }

    /** Restore a previously deprecated document. */
    @Transactional
    public DocumentResponse restore(Long id, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (!doc.getOwner().getEmail().equals(userEmail) && !isAdmin(user))
            throw new AccessDeniedException("Only the owner or an admin can restore this document");
        doc.setStatus(Document.DocumentStatus.ACTIVE);
        Document saved = documentRepository.save(doc);
        auditService.log(userEmail, null, AuditLog.Action.DOCUMENT_RESTORE,
                "DOCUMENT", id, "Restored \"" + doc.getTitle() + "\"",
                null, null, 200);
        return toResponse(saved);
    }

    /** Back-compat alias for the old "delete" API — actually a soft deprecate. */
    @Transactional
    public void deleteDocument(Long id, String userEmail) {
        deprecate(id, "soft-deleted via DELETE", userEmail);
    }

    /** Admin-only hard delete. Removes the DB row and the file on disk. */
    @Transactional
    public void purge(Long id, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Only an admin can permanently delete a document");
        }
        String filePath = doc.getFilePath();
        documentRepository.delete(doc);
        try { fileStorageService.delete(filePath); }
        catch (Exception e) { log.warn("Could not remove file {}: {}", filePath, e.getMessage()); }
        auditService.log(userEmail, null, AuditLog.Action.DOCUMENT_DELETE,
                "DOCUMENT", id, "Permanently deleted \"" + doc.getTitle() + "\"",
                null, null, 200);
    }

    // ── Download / Preview ───────────────────────────────────────────────
    @Transactional
    public Resource downloadDocument(Long id, String userEmail) {
        Document doc  = findDoc(id);
        User     user = getUser(userEmail);
        checkReadAccess(doc, user);
        doc.setDownloadCount(doc.getDownloadCount() + 1);
        documentRepository.save(doc);
        auditService.log(userEmail, null, AuditLog.Action.DOCUMENT_DOWNLOAD,
                "DOCUMENT", id, "Downloaded \"" + doc.getTitle() + "\"",
                null, null, 200);
        return fileStorageService.loadAsResource(doc.getFilePath());
    }

    public Resource previewDocument(Long id, String userEmail) {
        Document doc  = findDoc(id);
        User     user = getUser(userEmail);
        checkReadAccess(doc, user);
        return fileStorageService.loadAsResource(doc.getFilePath());
    }

    // ── Versions ─────────────────────────────────────────────────────────
    public List<DocumentVersionResponse> getVersions(Long docId, String userEmail) {
        Document doc  = findDoc(docId);
        User     user = getUser(userEmail);
        checkReadAccess(doc, user);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(docId)
                .stream().map(this::toVersionResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentVersionResponse uploadNewVersion(Long docId, MultipartFile file,
                                                    String changeSummary, String userEmail) {
        Document doc  = findDoc(docId);
        User     user = getUser(userEmail);
        checkEditAccess(doc, user);

        String filePath = fileStorageService.storeWithUsername(file, doc.getOwner().getUsername());
        int newVersion  = doc.getCurrentVersion() + 1;

        DocumentVersion v = DocumentVersion.builder()
                .document(doc)
                .versionNumber(newVersion)
                .fileName(filePath.substring(filePath.lastIndexOf("/") + 1))
                .filePath(filePath)
                .fileSize(file.getSize())
                .changeSummary(changeSummary)
                .uploadedBy(user)
                .build();
        versionRepository.save(v);

        doc.setCurrentVersion(newVersion);
        doc.setFilePath(filePath);
        doc.setFileName(v.getFileName());
        doc.setFileSize(file.getSize());
        documentRepository.save(doc);
        auditService.log(userEmail, null, AuditLog.Action.VERSION_UPLOAD,
                "DOCUMENT", docId, "New version " + newVersion + " of \"" + doc.getTitle() + "\"",
                null, null, 201);
        return toVersionResponse(v);
    }

    @Transactional
    public DocumentResponse restoreVersion(Long docId, Long versionId, String userEmail) {
        Document        doc     = findDoc(docId);
        User            user    = getUser(userEmail);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        checkEditAccess(doc, user);

        int newVersion = doc.getCurrentVersion() + 1;
        DocumentVersion restored = DocumentVersion.builder()
                .document(doc)
                .versionNumber(newVersion)
                .fileName(version.getFileName())
                .filePath(version.getFilePath())
                .fileSize(version.getFileSize())
                .changeSummary("Restored from version " + version.getVersionNumber())
                .uploadedBy(user)
                .build();
        versionRepository.save(restored);

        doc.setCurrentVersion(newVersion);
        doc.setFilePath(version.getFilePath());
        doc.setFileName(version.getFileName());
        doc.setFileSize(version.getFileSize());
        return toResponse(documentRepository.save(doc));
    }

    public Resource downloadVersion(Long docId, Long versionId, String userEmail) {
        Document        doc     = findDoc(docId);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        User user = getUser(userEmail);
        checkReadAccess(doc, user);
        return fileStorageService.loadAsResource(version.getFilePath());
    }

    // ── Permissions / Sharing ────────────────────────────────────────────
    public List<DocumentPermissionResponse> getPermissions(Long docId, String userEmail) {
        Document doc  = findDoc(docId);
        User     user = getUser(userEmail);
        checkReadAccess(doc, user);
        return permissionRepository.findByDocumentId(docId)
                .stream().map(this::toPermissionResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentPermissionResponse shareDocument(Long docId, ShareDocumentRequest req, String granterEmail) {
        Document doc     = findDoc(docId);
        User     granter = getUser(granterEmail);
        checkEditAccess(doc, granter);

        User recipient = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + req.getEmail()));

        DocumentPermission perm = permissionRepository.findByDocumentIdAndUserId(docId, recipient.getId())
                .orElse(DocumentPermission.builder().document(doc).user(recipient).grantedBy(granter).build());
        perm.setPermission(req.getPermission());
        permissionRepository.save(perm);

        auditService.log(granterEmail, null, AuditLog.Action.DOCUMENT_SHARE,
                "DOCUMENT", docId,
                "Shared \"" + doc.getTitle() + "\" with " + recipient.getEmail()
                        + " (" + req.getPermission().name() + ")",
                null, null, 201);

        try { emailService.sendShareNotificationEmail(recipient, granter, doc.getTitle(), req.getPermission().name()); }
        catch (Exception e) { log.warn("Share notification email failed: {}", e.getMessage()); }

        return toPermissionResponse(perm);
    }

    @Transactional
    public DocumentPermissionResponse updatePermission(Long docId, Long userId,
                                                       DocumentPermission.PermissionType permission,
                                                       String requesterEmail) {
        Document doc  = findDoc(docId);
        User     user = getUser(requesterEmail);
        checkEditAccess(doc, user);
        DocumentPermission perm = permissionRepository.findByDocumentIdAndUserId(docId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        perm.setPermission(permission);
        return toPermissionResponse(permissionRepository.save(perm));
    }

    @Transactional
    public void removePermission(Long docId, Long userId, String requesterEmail) {
        Document doc  = findDoc(docId);
        User     user = getUser(requesterEmail);
        checkEditAccess(doc, user);
        permissionRepository.deleteByDocumentIdAndUserId(docId, userId);
    }

    // ── Access helpers ───────────────────────────────────────────────────
    private void checkReadAccess(Document doc, User user) {
        if (isAdmin(user) || doc.getOwner().getId().equals(user.getId())
                || Boolean.TRUE.equals(doc.getIsPublic())) return;
        if (permissionRepository.existsByDocumentIdAndUserId(doc.getId(), user.getId())) return;
        throw new AccessDeniedException("You don't have access to this document");
    }

    private void checkEditAccess(Document doc, User user) {
        if (isAdmin(user) || doc.getOwner().getId().equals(user.getId())) return;
        permissionRepository.findByDocumentIdAndUserId(doc.getId(), user.getId())
                .filter(p -> p.getPermission() == DocumentPermission.PermissionType.EDIT
                          || p.getPermission() == DocumentPermission.PermissionType.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("You don't have edit access to this document"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
    }

    // ── Mappers ──────────────────────────────────────────────────────────
    public DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .uploadPurpose(doc.getUploadPurpose())
                .fileName(doc.getFileName())
                .originalFileName(doc.getOriginalFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .mimeType(doc.getMimeType())
                .currentVersion(doc.getCurrentVersion())
                .status(doc.getStatus())
                .owner(authService.mapUserToResponse(doc.getOwner()))
                .tags(doc.getTags())
                .isPublic(doc.getIsPublic())
                .downloadCount(doc.getDownloadCount())
                .viewCount(doc.getViewCount())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion v) {
        return DocumentVersionResponse.builder()
                .id(v.getId())
                .versionNumber(v.getVersionNumber())
                .fileName(v.getFileName())
                .fileSize(v.getFileSize())
                .changeSummary(v.getChangeSummary())
                .uploadedBy(v.getUploadedBy() != null ? authService.mapUserToResponse(v.getUploadedBy()) : null)
                .createdAt(v.getCreatedAt())
                .build();
    }

    private DocumentPermissionResponse toPermissionResponse(DocumentPermission p) {
        return DocumentPermissionResponse.builder()
                .id(p.getId())
                .user(authService.mapUserToResponse(p.getUser()))
                .permission(p.getPermission())
                .expiresAt(p.getExpiresAt())
                .grantedAt(p.getGrantedAt())
                .grantedBy(p.getGrantedBy() != null ? authService.mapUserToResponse(p.getGrantedBy()) : null)
                .build();
    }

    private Document findDoc(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private String detectMime(MultipartFile file) {
        try { return tika.detect(file.getInputStream()); }
        catch (IOException e) { return file.getContentType() != null ? file.getContentType() : "application/octet-stream"; }
    }
}
