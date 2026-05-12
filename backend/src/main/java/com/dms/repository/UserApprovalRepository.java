package com.dms.repository;

import com.dms.entity.UserApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserApprovalRepository extends JpaRepository<UserApproval, Long> {
    Optional<UserApproval> findByUserId(Long userId);
    Page<UserApproval> findByStatus(UserApproval.ApprovalStatus status, Pageable pageable);
    long countByStatus(UserApproval.ApprovalStatus status);
}
