package com.dms.repository;

import com.dms.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    List<DocumentPermission> findByDocumentId(Long documentId);
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);
    void deleteByDocumentIdAndUserId(Long documentId, Long userId);
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);
}
