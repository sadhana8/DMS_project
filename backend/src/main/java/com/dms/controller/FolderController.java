package com.dms.controller;

import com.dms.dto.request.*;
import com.dms.dto.response.*;
import com.dms.service.impl.FolderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for folder CRUD.
 *
 * <p>Route summary:
 * <ul>
 *   <li>{@code GET    /api/folders}                – root folders visible to caller</li>
 *   <li>{@code GET    /api/folders/tree}           – caller's full folder tree</li>
 *   <li>{@code GET    /api/folders/{id}}           – one folder</li>
 *   <li>{@code GET    /api/folders/{id}/children}  – direct children</li>
 *   <li>{@code POST   /api/folders}                – create (ACCOUNT+)</li>
 *   <li>{@code PUT    /api/folders/{id}}           – rename / move / toggle public</li>
 *   <li>{@code DELETE /api/folders/{id}}           – delete if empty</li>
 *   <li>{@code DELETE /api/folders/{id}?recursive=true} – recursive delete (owner/admin)</li>
 * </ul>
 */
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderServiceImpl folderService;

    @GetMapping
    public ResponseEntity<List<FolderResponse>> roots(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(folderService.listRoots(ud.getUsername()));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<FolderResponse>> tree(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(folderService.getTree(ud.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getOne(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(folderService.getById(id, ud.getUsername()));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<FolderResponse>> children(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(folderService.listChildren(id, ud.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','ACCOUNT')")
    public ResponseEntity<FolderResponse> create(@Valid @RequestBody CreateFolderRequest req,
                                                  @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(folderService.create(req, ud.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateFolderRequest req,
                                                  @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(folderService.update(id, req, ud.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id,
                                              @RequestParam(defaultValue = "false") boolean recursive,
                                              @AuthenticationPrincipal UserDetails ud) {
        if (recursive) folderService.deleteRecursive(id, ud.getUsername());
        else           folderService.delete(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Folder deleted"));
    }
}
