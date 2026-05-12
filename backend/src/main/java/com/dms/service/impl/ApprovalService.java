package com.dms.service.impl;

import com.dms.dto.request.ApprovalRequest;
import com.dms.dto.response.*;
import com.dms.entity.*;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.*;
import com.dms.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final UserApprovalRepository approvalRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final NotificationService notifService;
    private final AuditService auditService;
    private final AuthServiceImpl authService;

    // ── List pending/all approvals ─────────────────────────────────────────
    public Page<UserApprovalResponse> list(UserApproval.ApprovalStatus status, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return approvalRepo.findByStatus(status, pg).map(this::toResponse);
    }

    public long countPending() {
        return approvalRepo.countByStatus(UserApproval.ApprovalStatus.PENDING);
    }

    // ── Admin approves or rejects ──────────────────────────────────────────
    @Transactional
    public UserApprovalResponse review(Long approvalId, ApprovalRequest req, String adminEmail) {
        UserApproval approval = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found: " + approvalId));

        approval.setStatus(req.getStatus());
        approval.setReviewedBy(adminEmail);
        approval.setReviewNote(req.getNote());
        approvalRepo.save(approval);

        User user = approval.getUser();

        if (req.getStatus() == UserApproval.ApprovalStatus.APPROVED) {
            user.setIsActive(true);
            userRepo.save(user);
            notifService.createForUser(user.getId(),
                    Notification.NotificationType.USER_APPROVED,
                    "Account Approved",
                    "Your account has been approved. You can now log in.",
                    "/dashboard");
            // Send approval email (best effort)
            try {
                emailService.sendAccountApprovedEmail(user);
            } catch (Exception ignored) {
            }
        } else {
            notifService.createForUser(user.getId(),
                    Notification.NotificationType.USER_REJECTED,
                    "Account Rejected",
                    "Your account was not approved. Reason: "
                    + (req.getNote() != null ? req.getNote() : "No reason provided"),
                    null);
        }

        auditService.log(adminEmail, AuditLog.Action.USER_ACTIVATE,
                "User account " + req.getStatus().name().toLowerCase() + ": " + user.getEmail());

        return toResponse(approval);
    }

    // ── Creates a new pending approval for a newly registered user ────────
    @Transactional
    public void createPending(User user) {
        approvalRepo.save(UserApproval.builder().user(user).build());
    }

    private UserApprovalResponse toResponse(UserApproval a) {
        return UserApprovalResponse.builder()
                .id(a.getId())
                .user(authService.mapUserToResponse(a.getUser()))
                .status(a.getStatus())
                .reviewedBy(a.getReviewedBy())
                .reviewNote(a.getReviewNote())
                .createdAt(a.getCreatedAt())
                .reviewedAt(a.getReviewedAt())
                .build();
    }
}
