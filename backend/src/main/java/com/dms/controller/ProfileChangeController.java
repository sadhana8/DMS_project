package com.dms.controller;

import com.dms.dto.request.ProfileChangeRequestBody;
import com.dms.dto.request.ProfileChangeReviewBody;
import com.dms.dto.response.ProfileChangeRequestResponse;
import com.dms.entity.ProfileChangeRequest.Status;
import com.dms.exception.BadRequestException;
import com.dms.service.impl.ProfileChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for the profile-change request workflow.
 *
 * <ul>
 * <li>{@code POST /api/profile-changes} — employee submits a request.</li>
 * <li>{@code GET  /api/profile-changes/mine} — employee lists their own
 * requests.</li>
 * <li>{@code GET  /api/profile-changes} — HR/Admin lists requests, optionally
 * filtered by status (default PENDING).</li>
 * <li>{@code GET  /api/profile-changes/count} — pending count, used by the HR
 * dashboard tile.</li>
 * <li>{@code PUT  /api/profile-changes/{id}/review} — HR/Admin approves or
 * rejects a pending request.</li>
 * </ul>
 */
@RestController
@RequestMapping("/profile-changes")
@RequiredArgsConstructor
public class ProfileChangeController {

    private final ProfileChangeRequestService service;

    @PostMapping
    public ResponseEntity<ProfileChangeRequestResponse> create(
            @Valid @RequestBody ProfileChangeRequestBody body,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(ud.getUsername(), body));
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ProfileChangeRequestResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(service.listMine(ud.getUsername(), page, size));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Page<ProfileChangeRequestResponse>> listForReview(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Status s;
        try {
            s = Status.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid status: " + status);
        }
        return ResponseEntity.ok(service.listByStatus(s, page, size));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("pending", service.countPending()));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<ProfileChangeRequestResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody ProfileChangeReviewBody body,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(service.review(id, body, ud.getUsername()));
    }
}
