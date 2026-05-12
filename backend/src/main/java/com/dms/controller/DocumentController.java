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
 * REST controller for documents.
 *
 * <p>Endpoint summary:
 * <ul>
 *   <li>{@code GET    /api/documents}                    – list accessible docs (paged)</li>
 *   <li>{@code GET    /api/documents/search?query=}      – full-text search</li>
 *   <li>{@code GET    /api/documents/deprecated}         – admin: archived docs</li>
 *   <li>{@code GET    /api/documents/by-folder/{id}}     – docs inside a folder</li>
 *   <li>{@code GET    /api/documents/{id}}               – single doc</li>
 *   <li>{@code POST   /api/documents/upload}             – multipart upload (ACCOUNT+)</li>
 *   <li>{@code PUT    /api/documents/{id}}               – update metadata</li>
 *   <li>{@code PUT    /api/documents/{id}/move?folderId=}– move to folder</li>
 *   <li>{@code POST   /api/documents/{id}/deprecate}     – soft-deprecate</li>
 *   <li>{@code POST   /api/documents/{id}/restore}       – restore a deprecated doc</li>
 *   <li>{@code DELETE /api/documents/{id}}               – soft-delete (=deprecate)</li>
 *   <li>{@code DELETE /api/documents/{id}/purge}         – admin-only hard delete</li>
 *   <li>{@code GET    /api/documents/{id}/download}      – download current version</li>
 *   <li>{@code GET    /api/documents/{id}/preview}       – inline preview</li>
 *   <li>{@code GET    /api/documents/{id}/versions}      – list versions</li>
 *   <li>{@code POST   /api/documents/{id}/versions}      – upload new version</li>
 *   <li>{@code POST   /api/documents/{id}/versions/{v}/restore} – restore a prior version</li>
 *   <li>{@code GET    /api/documents/{id}/versions/{v}/download} – download a specific version</li>
 *   <li>{@code GET    /api/documents/{id}/permissions}   – list share grants</li>
 *   <li>{@code POST   /api/documents/{id}/permissions}   – share with user</li>
 *   <li>{@code PUT    /api/documents/{id}/permissions/{userId}} – change permission</li>
 *   <li>{@code DELETE /api/documents/{id}/permissions/{userId}} – revoke permission</li>
 * </ul>
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentServiceImpl documentService;

    // ── List / Search ────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> list(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(documentService.listDocuments(ud.getUsername(), page, size, status));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DocumentResponse>> search(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(documentService.searchDocuments(ud.getUsername(), query, page, size));
    }

    @GetMapping("/deprecated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DocumentResponse>> listDeprecated(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.listDeprecated(page, size));
    }

    @GetMapping("/by-folder/{folderId}")
    public ResponseEntity<Page<DocumentResponse>> listInFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.listInFolder(folderId, ud.getUsername(), page, size));
    }

    // ── Single document ──────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getDocument(id, ud.getUsername()));
    }

    // ── Upload ───────────────────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','ACCOUNT')")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file")                         MultipartFile file,
            @RequestParam(value = "title",       required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags",        required = false) String tags,
            @RequestParam(value = "isPublic",    required = false, defaultValue = "false") Boolean isPublic,
            @RequestParam(value = "folderId",    required = false) Long folderId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(file, title, description, tags,
                        isPublic, folderId, ud.getUsername()));
    }

    // ── Update / Move ────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.updateDocument(id, request, ud.getUsername()));
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<DocumentResponse> move(
            @PathVariable Long id,
            @RequestParam(required = false) Long folderId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.moveToFolder(id, folderId, ud.getUsername()));
    }

    // ── Deprecate / Restore ──────────────────────────────────────────────
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<DocumentResponse> deprecate(
            @PathVariable Long id,
            @RequestBody(required = false) DeprecateRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(documentService.deprecate(id, reason, ud.getUsername()));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<DocumentResponse> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.restore(id, ud.getUsername()));
    }

    /** Soft-delete: alias for deprecate. Kept for backward compatibility. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        documentService.deleteDocument(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Document deprecated (can be restored by admin)"));
    }

    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> purge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        documentService.purge(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Document permanently deleted"));
    }

    // ── Download / Preview ───────────────────────────────────────────────
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Resource resource = documentService.downloadDocument(id, ud.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        DocumentResponse meta = documentService.getDocument(id, ud.getUsername());
        Resource resource     = documentService.previewDocument(id, ud.getUsername());
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(meta.getMimeType()); }
        catch (Exception e) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }

    // ── Versions ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getVersions(id, ud.getUsername()));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','ACCOUNT')")
    public ResponseEntity<DocumentVersionResponse> uploadVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeSummary", required = false, defaultValue = "") String changeSummary,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadNewVersion(id, file, changeSummary, ud.getUsername()));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','HR','ACCOUNT')")
    public ResponseEntity<DocumentResponse> restoreVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.restoreVersion(id, versionId, ud.getUsername()));
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetails ud) {
        Resource resource = documentService.downloadVersion(id, versionId, ud.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // ── Permissions ──────────────────────────────────────────────────────
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<DocumentPermissionResponse>> getPermissions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(documentService.getPermissions(id, ud.getUsername()));
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<DocumentPermissionResponse> share(
            @PathVariable Long id,
            @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.shareDocument(id, request, ud.getUsername()));
    }

    @PutMapping("/{id}/permissions/{userId}")
    public ResponseEntity<DocumentPermissionResponse> updatePermission(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        DocumentPermission.PermissionType perm =
                DocumentPermission.PermissionType.valueOf(body.get("permission"));
        return ResponseEntity.ok(documentService.updatePermission(id, userId, perm, ud.getUsername()));
    }

    @DeleteMapping("/{id}/permissions/{userId}")
    public ResponseEntity<ApiResponse> removePermission(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails ud) {
        documentService.removePermission(id, userId, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Permission removed"));
    }
}
