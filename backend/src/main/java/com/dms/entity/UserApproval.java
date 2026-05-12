package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Tracks admin approval for newly registered users.
 * When requireApproval=true in settings, new users are PENDING until an admin approves or rejects.
 */
@Entity
@Table(name = "user_approvals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserApproval {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public enum ApprovalStatus { PENDING, APPROVED, REJECTED }
}
