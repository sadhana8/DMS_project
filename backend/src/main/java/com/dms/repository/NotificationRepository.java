package com.dms.repository;

import com.dms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value
            = "SELECT * FROM notifications "
            + "WHERE user_id = :uid "
            + "  AND (cast(:type as text) IS NULL OR type = cast(:type as text)) "
            + "  AND (cast(:isRead as boolean) IS NULL OR is_read = cast(:isRead as boolean)) "
            + "  AND (cast(:fromDt as timestamp) IS NULL OR created_at >= cast(:fromDt as timestamp)) "
            + "  AND (cast(:toDt   as timestamp) IS NULL OR created_at <= cast(:toDt   as timestamp)) "
            + "ORDER BY created_at DESC",
            countQuery
            = "SELECT count(*) FROM notifications "
            + "WHERE user_id = :uid "
            + "  AND (cast(:type as text) IS NULL OR type = cast(:type as text)) "
            + "  AND (cast(:isRead as boolean) IS NULL OR is_read = cast(:isRead as boolean)) "
            + "  AND (cast(:fromDt as timestamp) IS NULL OR created_at >= cast(:fromDt as timestamp)) "
            + "  AND (cast(:toDt   as timestamp) IS NULL OR created_at <= cast(:toDt   as timestamp))",
            nativeQuery = true)
    Page<Notification> findFiltered(@Param("uid") Long uid,
            @Param("type") String type,
            @Param("isRead") Boolean isRead,
            @Param("fromDt") LocalDateTime from,
            @Param("toDt") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :uid AND n.isRead = false")
    long countUnread(@Param("uid") Long uid);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :uid AND n.isRead = false")
    void markAllRead(@Param("uid") Long uid);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipient.id = :uid")
    void markOneRead(@Param("id") Long id, @Param("uid") Long uid);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    void deleteOldRead(@Param("cutoff") LocalDateTime cutoff);
}
