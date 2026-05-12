package com.dms.repository;

import com.dms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:user IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%',:user,'%'))) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:from IS NULL OR a.createdAt >= :from) AND " +
           "(:to IS NULL OR a.createdAt <= :to) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("user") String user,
                          @Param("action") AuditLog.Action action,
                          @Param("entityType") String entityType,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a " +
           "WHERE a.createdAt >= :since GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> topUsersByActivity(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " +
           "WHERE a.createdAt >= :since GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> actionCounts(@Param("since") LocalDateTime since);
}
