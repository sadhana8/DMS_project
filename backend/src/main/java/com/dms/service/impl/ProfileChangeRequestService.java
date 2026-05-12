package com.dms.service.impl;

import com.dms.dto.request.ProfileChangeRequestBody;
import com.dms.dto.request.ProfileChangeReviewBody;
import com.dms.dto.response.ProfileChangeRequestResponse;
import com.dms.entity.AuditLog;
import com.dms.entity.Notification;
import com.dms.entity.ProfileChangeRequest;
import com.dms.entity.ProfileChangeRequest.Status;
import com.dms.entity.RoleName;
import com.dms.entity.User;
import com.dms.exception.BadRequestException;
import com.dms.exception.ResourceNotFoundException;
import com.dms.repository.ProfileChangeRequestRepository;
import com.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages the lifecycle of {@link ProfileChangeRequest}.
 *
 * <ul>
 * <li>Employee creates a request — fanned out as a notification to all
 * HR/Admin.</li>
 * <li>HR/Admin reviews — approve applies the change to the user, reject does
 * not.</li>
 * <li>Either way, the requesting employee gets a notification on review.</li>
 * <li>Every action is audited.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfileChangeRequestService {

    private final ProfileChangeRequestRepository requestRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ── Create ──────────────────────────────────────────────────────────────
    @Transactional
    public ProfileChangeRequestResponse create(String userEmail, ProfileChangeRequestBody body) {
        if (!ProfileChangeRequestBody.ALLOWED_FIELDS.contains(body.getFieldName())) {
            throw new BadRequestException(
                    "Field '" + body.getFieldName() + "' cannot be requested for change. "
                    + "Allowed fields: " + ProfileChangeRequestBody.ALLOWED_FIELDS);
        }
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String oldValue = readField(user, body.getFieldName());
        if (oldValue != null && oldValue.equals(body.getNewValue())) {
            throw new BadRequestException("New value matches the current value — nothing to change.");
        }

        ProfileChangeRequest req = ProfileChangeRequest.builder()
                .user(user)
                .fieldName(body.getFieldName())
                .oldValue(oldValue)
                .newValue(body.getNewValue())
                .reason(body.getReason())
                .status(Status.PENDING)
                .build();
        requestRepo.save(req);

        // Notify HR + Admin: fan out via NotificationService — both roles will see it.
        try {
            notificationService.notifyAllByRoles(
                    java.util.List.of(RoleName.ROLE_ADMIN, RoleName.ROLE_HR),
                    Notification.NotificationType.SYSTEM,
                    "Profile change request",
                    user.getFirstName() + " " + user.getLastName()
                    + " requested a change to '" + prettyField(body.getFieldName()) + "'",
                    "/hr/change-requests"
            );
        } catch (Exception e) {
            log.warn("Could not fan-out profile-change notification: {}", e.getMessage());
        }

        auditService.log(userEmail, null, AuditLog.Action.USER_UPDATE,
                "PROFILE_CHANGE_REQUEST", req.getId(),
                "Requested change to '" + body.getFieldName() + "'",
                null, null, 201);

        return toResponse(req);
    }

    // ── Read ────────────────────────────────────────────────────────────────
    public Page<ProfileChangeRequestResponse> listMine(String userEmail, int page, int size) {
        User me = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return requestRepo.findByUserId(me.getId(), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public Page<ProfileChangeRequestResponse> listByStatus(Status status, int page, int size) {
        return requestRepo.findByStatus(status, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public long countPending() {
        return requestRepo.countByStatus(Status.PENDING);
    }

    // ── Review ──────────────────────────────────────────────────────────────
    @Transactional
    public ProfileChangeRequestResponse review(Long id, ProfileChangeReviewBody body, String reviewerEmail) {
        ProfileChangeRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (req.getStatus() != Status.PENDING) {
            throw new BadRequestException("This request has already been " + req.getStatus().name().toLowerCase() + ".");
        }

        boolean approve = Boolean.TRUE.equals(body.getApprove());
        req.setStatus(approve ? Status.APPROVED : Status.REJECTED);
        req.setReviewedBy(reviewerEmail);
        req.setReviewNote(body.getNote());
        req.setReviewedAt(LocalDateTime.now());

        if (approve) {
            User u = req.getUser();
            applyApproved(u, req.getFieldName(), req.getNewValue());
            userRepo.save(u);
        }
        requestRepo.save(req);

        // Notify the requesting employee
        try {
            notificationService.createForUser(req.getUser().getId(),
                    Notification.NotificationType.SYSTEM,
                    approve ? "Profile change approved" : "Profile change rejected",
                    approve
                            ? "Your request to change '" + prettyField(req.getFieldName()) + "' was approved."
                            : "Your request to change '" + prettyField(req.getFieldName()) + "' was rejected"
                            + (body.getNote() != null && !body.getNote().isBlank()
                            ? ": " + body.getNote() : "."),
                    "/profile"
            );
        } catch (Exception e) {
            log.warn("Could not notify requester: {}", e.getMessage());
        }

        auditService.log(reviewerEmail, null, AuditLog.Action.USER_UPDATE,
                "PROFILE_CHANGE_REQUEST", req.getId(),
                (approve ? "Approved" : "Rejected") + " profile change request for "
                + req.getUser().getEmail() + " — field: " + req.getFieldName(),
                null, null, 200);

        return toResponse(req);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    /**
     * Reads the current value of a field on the User, for old-value capture.
     */
    private String readField(User u, String fieldName) {
        return switch (fieldName) {
            case "phoneNumber" ->
                u.getPhoneNumber();
            case "address" ->
                u.getAddress();
            default ->
                null;
        };
    }

    /**
     * Writes the approved new value to the User entity.
     */
    private void applyApproved(User u, String fieldName, String newValue) {
        switch (fieldName) {
            case "phoneNumber" ->
                u.setPhoneNumber(newValue);
            case "address" ->
                u.setAddress(newValue);
            default ->
                throw new BadRequestException("Unsupported field: " + fieldName);
        }
    }

    private String prettyField(String fieldName) {
        return switch (fieldName) {
            case "phoneNumber" ->
                "Phone number";
            case "address" ->
                "Address";
            default ->
                fieldName;
        };
    }

    private ProfileChangeRequestResponse toResponse(ProfileChangeRequest r) {
        User u = r.getUser();
        return ProfileChangeRequestResponse.builder()
                .id(r.getId())
                .userId(u.getId())
                .userEmail(u.getEmail())
                .userFirstName(u.getFirstName())
                .userLastName(u.getLastName())
                .fieldName(r.getFieldName())
                .oldValue(r.getOldValue())
                .newValue(r.getNewValue())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .reviewedBy(r.getReviewedBy())
                .reviewNote(r.getReviewNote())
                .reviewedAt(r.getReviewedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
