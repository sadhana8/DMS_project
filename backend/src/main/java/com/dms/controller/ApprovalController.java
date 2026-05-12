package com.dms.controller;

import com.dms.dto.request.ApprovalRequest;
import com.dms.dto.response.*;
import com.dms.entity.UserApproval;
import com.dms.service.impl.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/approvals")
@PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService svc;

    @GetMapping
    public ResponseEntity<Page<UserApprovalResponse>> list(
            @RequestParam(defaultValue = "PENDING") UserApproval.ApprovalStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(svc.list(status, page, size));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("pending", svc.countPending()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserApprovalResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(svc.review(id, req, ud.getUsername()));
    }
}
