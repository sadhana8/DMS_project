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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for all document-related operations.
 *
 * <h2>Deprecation instead of deletion</h2>
 * Documents are <b>never hard-deleted</b>. Calling {@link #deprecateDocument}
 * sets {@link DeprecationStatus} to {@link DeprecationStatus#DEPRECATED},
 * records the reason and who performed the action, and excludes the document
 * from all standard queries. The file bytes on disk are preserved.
 * {@link #restoreDocument} reverses the operation.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final AuthServiceImpl authService;
    private final Tika tika = new Tika();

    // ── List / Search ─────────────────────────────────────────────────────
    public Page<DocumentResponse> listDocuments(String userEmail, int page, int size, String status) {
        User user = getUser(userEmail);
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.findAccessibleByUser(user, pg).map(this::toResponse);
    }

    public Page<DocumentResponse> searchDocuments(String userEmail, String query, int page, int size) {
        User user = getUser(userEmail);
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.searchDocuments(query, user, pg).map(this::toResponse);
    }

    public Page<DocumentResponse> listAllDocuments(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return documentRepository.findAllActive(pg).map(this::toResponse);
    }

    /**
     * Returns all deprecated documents (admin view).
     *
     * @param page zero-based page index
     * @param size page size
     * @return a {@link Page} of deprecated {@link DocumentResponse} DTOs
     */
    public Page<DocumentResponse> listDeprecatedDocuments(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("deprecatedAt").descending());
        return documentRepository.findAllDeprecated(pg).map(this::toResponse);
    }

    // ── Get single ────────────────────────────────────────────────────────
    @Transactional
    public DocumentResponse getDocument(Long id, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (doc.isDeprecated()) {
            throw new ResourceNotFoundException("Document not found: " + id);
        }
        checkReadAccess(doc, user);
        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);
        return toResponse(doc);
    }

    // ── Upload ────────────────────────────────────────────────────────────
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title, String description,
            String tags, Boolean isPublic, String userEmail) {
        User owner = getUser(userEmail);
        String mimeType = detectMime(file);
        String filePath = fileStorageService.store(file, "documents/" + owner.getId());
        long fileSize = file.getSize();
        String origName = file.getOriginalFilename();
        String ext = origName != null && origName.contains(".")
                ? origName.substring(origName.lastIndexOf(".") + 1).toUpperCase() : "FILE";

        Document doc = Document.builder()
                .title(title != null ? title : origName)
                .description(description)
                .fileName(filePath.substring(filePath.lastIndexOf("/") + 1))
                .originalFileName(origName)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileType(ext)
                .mimeType(mimeType)
                .owner(owner)
                .tags(tags)
                .isPublic(isPublic != null && isPublic)
                .currentVersion(1)
                .build();
        documentRepository.save(doc);

        versionRepository.save(DocumentVersion.builder()
                .document(doc).versionNumber(1).fileName(doc.getFileName())
                .filePath(filePath).fileSize(fileSize)
                .changeSummary("Initial upload").uploadedBy(owner).build());

        log.info("Document uploaded: {} by {}", doc.getTitle(), userEmail);
        return toResponse(doc);
    }

    // ── Update ────────────────────────────────────────────────────────────
    @Transactional
    public DocumentResponse updateDocument(Long id, UpdateDocumentRequest req, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (doc.isDeprecated()) {
            throw new ResourceNotFoundException("Document not found: " + id);
        }
        checkEditAccess(doc, user);
        if (req.getTitle() != null) {
            doc.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            doc.setDescription(req.getDescription());
        }
        if (req.getTags() != null) {
            doc.setTags(req.getTags());
        }
        if (req.getIsPublic() != null) {
            doc.setIsPublic(req.getIsPublic());
        }
        if (req.getStatus() != null) {
            try {
                doc.setStatus(Document.DocumentStatus.valueOf(req.getStatus()));
            } catch (Exception ignored) {
            }
        }
        return toResponse(documentRepository.save(doc));
    }

    // ── Deprecate (replaces delete) ───────────────────────────────────────
    /**
     * <b>Deprecates</b> a document — the soft, reversible alternative to
     * deletion.
     *
     * <p>
     * What happens:
     * <ul>
     * <li>{@link DeprecationStatus} is set to
     * {@link DeprecationStatus#DEPRECATED}.</li>
     * <li>{@code deprecatedAt}, {@code deprecationReason}, and
     * {@code deprecatedBy} are recorded for the audit trail.</li>
     * <li>The document is excluded from all standard list, search, download,
     * and preview endpoints.</li>
     * <li>The physical file on disk is <b>not</b> deleted — use
     * {@link #restoreDocument} to bring it back at any time.</li>
     * </ul>
     *
     * @param id the document ID
     * @param reason human-readable reason for deprecation
     * @param userEmail the e-mail of the user performing the action (must be
     * the owner or an admin)
     * @throws AccessDeniedException if the user is not the owner or admin
     * @throws IllegalStateException if the document is already deprecated
     */
    @Transactional
    public DocumentResponse deprecateDocument(Long id, String reason, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (!doc.getOwner().getEmail().equals(userEmail) && !isAdmin(user)) {
            throw new AccessDeniedException("Only the owner or admin can deprecate this document");
        }
        if (doc.isDeprecated()) {
            throw new IllegalStateException("Document is already deprecated");
        }

        doc.setDeprecationStatus(DeprecationStatus.DEPRECATED);
        doc.setDeprecatedAt(LocalDateTime.now());
        doc.setDeprecationReason(reason);
        doc.setDeprecatedBy(user.getUsername());
        log.info("Document deprecated: {} by {}", doc.getTitle(), userEmail);
        return toResponse(documentRepository.save(doc));
    }

    /**
     * <b>Restores</b> a previously deprecated document back to active status.
     *
     * <p>
     * What happens:
     * <ul>
     * <li>{@link DeprecationStatus} is reset to
     * {@link DeprecationStatus#ACTIVE}.</li>
     * <li>All deprecation audit fields are cleared.</li>
     * <li>The document reappears in standard list, search, and access
     * endpoints.</li>
     * </ul>
     *
     * @param id the document ID
     * @param userEmail the e-mail of the admin performing the restore
     * @return the restored {@link DocumentResponse}
     * @throws IllegalStateException if the document is not currently deprecated
     */
    @Transactional
    public DocumentResponse restoreDocument(Long id, String userEmail) {
        Document doc = findDoc(id);
        if (!doc.isDeprecated()) {
            throw new IllegalStateException("Document is not deprecated");
        }
        doc.setDeprecationStatus(DeprecationStatus.ACTIVE);
        doc.setDeprecatedAt(null);
        doc.setDeprecationReason(null);
        doc.setDeprecatedBy(null);
        log.info("Document restored: {} by {}", doc.getTitle(), userEmail);
        return toResponse(documentRepository.save(doc));
    }

    // ── Download / Preview ────────────────────────────────────────────────
    @Transactional
    public Resource downloadDocument(Long id, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (doc.isDeprecated()) {
            throw new ResourceNotFoundException("Document not found: " + id);
        }
        checkReadAccess(doc, user);
        doc.setDownloadCount(doc.getDownloadCount() + 1);
        documentRepository.save(doc);
        return fileStorageService.loadAsResource(doc.getFilePath());
    }

    public Resource previewDocument(Long id, String userEmail) {
        Document doc = findDoc(id);
        User user = getUser(userEmail);
        if (doc.isDeprecated()) {
            throw new ResourceNotFoundException("Document not found: " + id);
        }
        checkReadAccess(doc, user);
        return fileStorageService.loadAsResource(doc.getFilePath());
    }

    // ── Versions ──────────────────────────────────────────────────────────
    public List<DocumentVersionResponse> getVersions(Long docId, String userEmail) {
        Document doc = findDoc(docId);
        checkReadAccess(doc, getUser(userEmail));
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(docId)
                .stream().map(this::toVersionResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentVersionResponse uploadNewVersion(Long docId, MultipartFile file,
            String changeSummary, String userEmail) {
        Document doc = findDoc(docId);
        User user = getUser(userEmail);
        if (doc.isDeprecated()) {
            throw new ResourceNotFoundException("Document not found: " + docId);
        }
        checkEditAccess(doc, user);
        String filePath = fileStorageService.store(file, "documents/" + doc.getOwner().getId());
        int newVersion = doc.getCurrentVersion() + 1;
        DocumentVersion v = DocumentVersion.builder()
                .document(doc).versionNumber(newVersion)
                .fileName(filePath.substring(filePath.lastIndexOf("/") + 1))
                .filePath(filePath).fileSize(file.getSize())
                .changeSummary(changeSummary).uploadedBy(user).build();
        versionRepository.save(v);
        doc.setCurrentVersion(newVersion);
        doc.setFilePath(filePath);
        doc.setFileName(v.getFileName());
        doc.setFileSize(file.getSize());
        documentRepository.save(doc);
        return toVersionResponse(v);
    }

    @Transactional
    public DocumentResponse restoreVersion(Long docId, Long versionId, String userEmail) {
        Document doc = findDoc(docId);
        User user = getUser(userEmail);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        checkEditAccess(doc, user);
        int newVersion = doc.getCurrentVersion() + 1;
        versionRepository.save(DocumentVersion.builder()
                .document(doc).versionNumber(newVersion)
                .fileName(version.getFileName()).filePath(version.getFilePath())
                .fileSize(version.getFileSize())
                .changeSummary("Restored from version " + version.getVersionNumber())
                .uploadedBy(user).build());
        doc.setCurrentVersion(newVersion);
        doc.setFilePath(version.getFilePath());
        doc.setFileName(version.getFileName());
        doc.setFileSize(version.getFileSize());
        return toResponse(documentRepository.save(doc));
    }

    public Resource downloadVersion(Long docId, Long versionId, String userEmail) {
        Document doc = findDoc(docId);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        checkReadAccess(doc, getUser(userEmail));
        return fileStorageService.loadAsResource(version.getFilePath());
    }

    // ── Permissions ───────────────────────────────────────────────────────
    public List<DocumentPermissionResponse> getPermissions(Long docId, String userEmail) {
        Document doc = findDoc(docId);
        checkReadAccess(doc, getUser(userEmail));
        return permissionRepository.findByDocumentId(docId)
                .stream().map(this::toPermissionResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentPermissionResponse shareDocument(Long docId, ShareDocumentRequest req, String granterEmail) {
        Document doc = findDoc(docId);
        User granter = getUser(granterEmail);
        checkEditAccess(doc, granter);
        User recipient = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getEmail()));
        DocumentPermission perm = permissionRepository.findByDocumentIdAndUserId(docId, recipient.getId())
                .orElse(DocumentPermission.builder().document(doc).user(recipient).grantedBy(granter).build());
        perm.setPermission(req.getPermission());
        permissionRepository.save(perm);
        try {
            emailService.sendShareNotificationEmail(recipient, granter, doc.getTitle(), req.getPermission().name());
        } catch (Exception e) {
            log.warn("Share email failed: {}", e.getMessage());
        }
        return toPermissionResponse(perm);
    }

    @Transactional
    public DocumentPermissionResponse updatePermission(Long docId, Long userId,
            DocumentPermission.PermissionType permission,
            String requesterEmail) {
        Document doc = findDoc(docId);
        checkEditAccess(doc, getUser(requesterEmail));
        DocumentPermission perm = permissionRepository.findByDocumentIdAndUserId(docId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        perm.setPermission(permission);
        return toPermissionResponse(permissionRepository.save(perm));
    }

    @Transactional
    public void removePermission(Long docId, Long userId, String requesterEmail) {
        Document doc = findDoc(docId);
        checkEditAccess(doc, getUser(requesterEmail));
        permissionRepository.deleteByDocumentIdAndUserId(docId, userId);
    }

    // ── Access helpers ────────────────────────────────────────────────────
    private void checkReadAccess(Document doc, User user) {
        if (isAdmin(user) || doc.getOwner().getId().equals(user.getId()) || doc.getIsPublic()) {
            return;
        }
        if (permissionRepository.existsByDocumentIdAndUserId(doc.getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("You don't have access to this document");
    }

    private void checkEditAccess(Document doc, User user) {
        if (isAdmin(user) || doc.getOwner().getId().equals(user.getId())) {
            return;
        }
        permissionRepository.findByDocumentIdAndUserId(doc.getId(), user.getId())
                .filter(p -> p.getPermission() == DocumentPermission.PermissionType.EDIT
                || p.getPermission() == DocumentPermission.PermissionType.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("You don't have edit access"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
    }

    // ── Mappers ───────────────────────────────────────────────────────────
    public DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId()).title(doc.getTitle()).description(doc.getDescription())
                .fileName(doc.getFileName()).originalFileName(doc.getOriginalFileName())
                .fileSize(doc.getFileSize()).fileType(doc.getFileType()).mimeType(doc.getMimeType())
                .currentVersion(doc.getCurrentVersion()).status(doc.getStatus())
                .owner(authService.mapUserToResponse(doc.getOwner()))
                .tags(doc.getTags()).isPublic(doc.getIsPublic())
                .downloadCount(doc.getDownloadCount()).viewCount(doc.getViewCount())
                .createdAt(doc.getCreatedAt()).updatedAt(doc.getUpdatedAt()).build();
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion v) {
        return DocumentVersionResponse.builder()
                .id(v.getId()).versionNumber(v.getVersionNumber())
                .fileName(v.getFileName()).fileSize(v.getFileSize())
                .changeSummary(v.getChangeSummary())
                .uploadedBy(v.getUploadedBy() != null ? authService.mapUserToResponse(v.getUploadedBy()) : null)
                .createdAt(v.getCreatedAt()).build();
    }

    private DocumentPermissionResponse toPermissionResponse(DocumentPermission p) {
        return DocumentPermissionResponse.builder()
                .id(p.getId()).user(authService.mapUserToResponse(p.getUser()))
                .permission(p.getPermission()).expiresAt(p.getExpiresAt())
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
        try {
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        }
    }
}
