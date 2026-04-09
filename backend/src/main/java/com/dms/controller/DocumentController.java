package com.dms.controller;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.entity.DocumentPermission;
import com.dms.service.impl.DocumentServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for all document operations.
 *
 * <p>
 * Base path: {@code /api/documents}
 *
 * <h2>No hard deletion</h2>
 * The hard {@code DELETE /{id}} endpoint is replaced by:
 * <ul>
 * <li>{@code PUT /{id}/deprecate} – soft-deprecates the document
 * (reversible).</li>
 * <li>{@code PUT /{id}/restore} – restores a deprecated document (admin
 * only).</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentServiceImpl documentService;

    // ── List / Search ─────────────────────────────────────────────────────
    /**
     * Returns all accessible, non-deprecated documents for the authenticated
     * user.
     */
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> list(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(documentService.listDocuments(ud.getUsername(), page, size, status));
    }

    /**
     * Full-text search across title, description, and tags.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DocumentResponse>> search(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(documentService.searchDocuments(ud.getUsername(), query, page, size));
    }

    /**
     * Returns a single non-deprecated document and increments its view counter.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getDocument(id, ud.getUsername()));
    }

    // ── Upload ────────────────────────────────────────────────────────────
    /**
     * Uploads a new document. Requires EDITOR role or above.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EDITOR')")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(file, title, description, tags, isPublic, ud.getUsername()));
    }

    // ── Update ────────────────────────────────────────────────────────────
    /**
     * Updates document metadata. Only non-null fields are applied.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.updateDocument(id, request, ud.getUsername()));
    }

    // ── Deprecate & Restore (replaces DELETE) ─────────────────────────────
    /**
     * <b>Deprecates</b> a document — soft, reversible, no data lost.
     *
     * <p>
     * {@code PUT /api/documents/{id}/deprecate}
     *
     * <p>
     * The document is hidden from all standard queries and download/preview
     * endpoints return {@code 404}. The file on disk is preserved. Only the
     * owner or an ADMIN may deprecate a document. Use {@code PUT /{id}/restore}
     * to undo.
     *
     * <p>
     * Request body (optional):
     * <pre>{@code { "reason": "Superseded by version 2" }}</pre>
     *
     * @param id the document ID
     * @param request optional deprecation reason
     * @param ud the authenticated user
     * @return {@code 200 OK} with the updated {@link DocumentResponse}
     */
    @PutMapping("/{id}/deprecate")
    public ResponseEntity<DocumentResponse> deprecate(
            @PathVariable Long id,
            @RequestBody(required = false) DeprecateRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(documentService.deprecateDocument(id, reason, ud.getUsername()));
    }

    /**
     * <b>Restores</b> a deprecated document back to active status.
     *
     * <p>
     * {@code PUT /api/documents/{id}/restore}
     *
     * <p>
     * Resets {@link com.dms.entity.DeprecationStatus} to ACTIVE so the document
     * reappears in standard queries and can be downloaded again. Only admins
     * may restore documents.
     *
     * @param id the document ID
     * @param ud the authenticated admin
     * @return {@code 200 OK} with the restored {@link DocumentResponse}
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.restoreDocument(id, ud.getUsername()));
    }

    // ── Download / Preview ────────────────────────────────────────────────
    /**
     * Downloads the current version as a file attachment.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        Resource resource = documentService.downloadDocument(id, ud.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Streams the current version inline for browser preview.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        DocumentResponse meta = documentService.getDocument(id, ud.getUsername());
        Resource resource = documentService.previewDocument(id, ud.getUsername());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(meta.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }

    // ── Versions ──────────────────────────────────────────────────────────
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getVersions(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getVersions(id, ud.getUsername()));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EDITOR')")
    public ResponseEntity<DocumentVersionResponse> uploadVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeSummary", defaultValue = "") String changeSummary,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadNewVersion(id, file, changeSummary, ud.getUsername()));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EDITOR')")
    public ResponseEntity<DocumentResponse> restoreVersion(
            @PathVariable Long id, @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.restoreVersion(id, versionId, ud.getUsername()));
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable Long id, @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetails ud) {
        Resource resource = documentService.downloadVersion(id, versionId, ud.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // ── Permissions ───────────────────────────────────────────────────────
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<DocumentPermissionResponse>> getPermissions(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getPermissions(id, ud.getUsername()));
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<DocumentPermissionResponse> share(
            @PathVariable Long id, @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.shareDocument(id, request, ud.getUsername()));
    }

    @PutMapping("/{id}/permissions/{userId}")
    public ResponseEntity<DocumentPermissionResponse> updatePermission(
            @PathVariable Long id, @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        DocumentPermission.PermissionType perm
                = DocumentPermission.PermissionType.valueOf(body.get("permission"));
        return ResponseEntity.ok(documentService.updatePermission(id, userId, perm, ud.getUsername()));
    }

    @DeleteMapping("/{id}/permissions/{userId}")
    public ResponseEntity<ApiResponse> removePermission(
            @PathVariable Long id, @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails ud) {
        documentService.removePermission(id, userId, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Permission removed"));
    }
}
