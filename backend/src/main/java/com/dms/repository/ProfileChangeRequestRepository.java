package com.dms.repository;

import com.dms.entity.ProfileChangeRequest;
import com.dms.entity.ProfileChangeRequest.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileChangeRequestRepository extends JpaRepository<ProfileChangeRequest, Long> {

    Page<ProfileChangeRequest> findByStatus(Status status, Pageable pageable);

    Page<ProfileChangeRequest> findByUserId(Long userId, Pageable pageable);

    long countByStatus(Status status);
}
