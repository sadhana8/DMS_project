package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks an employee's request to change a field on their own profile.
 *
 * <p>
 * Lifecycle:
 * <ol>
 * <li>Employee submits a request — status PENDING.</li>
 * <li>HR or Admin reviews — sets status to APPROVED or REJECTED.</li>
 * <li>If APPROVED, the actual {@code User} field is updated and the request is
 * closed (no further state changes).</li>
 * <li>If REJECTED, a {@code reviewNote} explains why; the user can submit a new
 * request if they want.</li>
 * </ol>
 *
 * <p>
 * The set of allowed {@code field} values is restricted by
 * {@link com.dms.dto.request.ProfileChangeRequestBody#ALLOWED_FIELDS} —
 * currently {@code phoneNumber} and {@code address}.
 */
@Entity
@Table(name = "profile_change_requests", indexes = {
    @Index(name = "idx_pcr_user", columnList = "user_id"),
    @Index(name = "idx_pcr_status", columnList = "status"),
    @Index(name = "idx_pcr_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The employee who submitted the request.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The User entity field name being changed (e.g. "phoneNumber", "address").
     */
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    /**
     * Value before the change — captured at submission time, for audit/history.
     */
    @Column(name = "old_value", length = 500)
    private String oldValue;

    /**
     * Requested new value.
     */
    @Column(name = "new_value", length = 500)
    private String newValue;

    /**
     * Optional free-text from the employee explaining why they're requesting
     * this.
     */
    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Email of the HR/Admin who approved or rejected. Null while PENDING.
     */
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    /**
     * Optional note from the reviewer (especially useful for rejections).
     */
    @Column(name = "review_note", length = 500)
    private String reviewNote;

    /**
     * When the review action happened. Null while PENDING.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
