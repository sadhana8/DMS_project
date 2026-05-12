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

    @Query(value
            = "SELECT * FROM audit_logs "
            + "WHERE (cast(:user as text) IS NULL OR LOWER(performed_by) LIKE '%' || cast(:user as text) || '%') "
            + "  AND (cast(:action as text) IS NULL OR action = cast(:action as text)) "
            + "  AND (cast(:entityType as text) IS NULL OR entity_type = cast(:entityType as text)) "
            + "  AND (cast(:fromDt as timestamp) IS NULL OR created_at >= cast(:fromDt as timestamp)) "
            + "  AND (cast(:toDt as timestamp) IS NULL OR created_at <= cast(:toDt as timestamp)) "
            + "ORDER BY created_at DESC",
            countQuery
            = "SELECT count(*) FROM audit_logs "
            + "WHERE (cast(:user as text) IS NULL OR LOWER(performed_by) LIKE '%' || cast(:user as text) || '%') "
            + "  AND (cast(:action as text) IS NULL OR action = cast(:action as text)) "
            + "  AND (cast(:entityType as text) IS NULL OR entity_type = cast(:entityType as text)) "
            + "  AND (cast(:fromDt as timestamp) IS NULL OR created_at >= cast(:fromDt as timestamp)) "
            + "  AND (cast(:toDt as timestamp) IS NULL OR created_at <= cast(:toDt as timestamp))",
            nativeQuery = true)
    Page<AuditLog> search(@Param("user") String user,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("fromDt") LocalDateTime from,
            @Param("toDt") LocalDateTime to,
            Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> topUsersByActivity(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> actionCounts(@Param("since") LocalDateTime since);
}

